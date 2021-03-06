package vSphere;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import com.vmware.vim25.GuestNicInfo.*;
import gui.GuiHelper;
import gui.MainWindow.MainGUI;

import javax.annotation.Nullable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class ManagedEntityWrapper implements Comparable<ManagedEntityWrapper> {
    public HashSet<String> meChildrenKey, vmsOfThisNetwork;
    public String meKey, meParentKey;
    public boolean isNetworked;
    private boolean fullPath;
    private Network[] nets;
    private String type;
    private String name;
    private Type meType;
    String path;

    /**
     * Constructor will figure out what type of ManagedEntity it is and handle it appropriately
     * @param me ManagedEntity we want to wrap
     * @param fullPath If set to true - toString will return fullPath
     */
    public ManagedEntityWrapper(ManagedEntity me, boolean fullPath) {
        init(me, fullPath);
    }

    /**
     * Constructor will figure out what type of ManagedEntity it is and handle it appropriately
     * @param me ManagedEntity we want to wrap
     */
    public ManagedEntityWrapper(ManagedEntity me) {
        init(me, false);
    }

    private void init(@Nullable ManagedEntity me, boolean fullPath) {
        this.fullPath = fullPath;

        if(me == null) {
            this.type = "Network";
            this.isNetworked = false;
            this.meType = Type.NETWORK;
            this.name = "Not Networked";
            return;
        } else {
            this.name = me.getName();
            this.type = me.getMOR().type;
            this.meKey = me.toString();
            if(me.getParent() != null)
                this.meParentKey = me.getParent().toString();
        }

        //this.me = me;

        switch (type) {
            case "VirtualMachine":
                VirtualMachineConfigInfo vmci = ((VirtualMachine) me).getConfig();
                if (vmci.template) {
                    type += " (Template)";
                    meType = Type.TEMPLATE;
                } else {
                    meType = Type.VM;
                }

                vSphere.VirtualMachinesMap.putIfAbsent(meKey, (VirtualMachine)me);
                vSphere.VirtualMachinesMewMap.putIfAbsent(meKey, this);
                if(meParentKey != null) {
                    ManagedEntityWrapper parentFolderMew = vSphere.FolderMewMap.get(meParentKey);
                    if(parentFolderMew.meChildrenKey != null)
                        parentFolderMew.meChildrenKey.add(meKey);
                }
                isNetworked = true;
                try {
                    VirtualMachine vm = (VirtualMachine)me;
                    nets = vm.getNetworks();
                    if(nets == null || nets.length == 0) {
                        isNetworked = false;
                    } else {
                        for(Network n : nets) {
                            vSphere.NetworkMewMap.get(n.toString()).vmsOfThisNetwork.add(meKey);
                        }
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
                break;
            case "Folder":
                meType = Type.FOLDER;
                meChildrenKey = new HashSet<>();
                try {
                    ManagedEntity[] meChildren = ((Folder) me).getChildEntity();
                    if(meChildren == null || meChildren.length == 0) break;
                    for (ManagedEntity meChild : meChildren) {
                        if(meChild != null)
                            meChildrenKey.add(meChild.toString());
                    }
                } catch (RemoteException runtimeFault) {
                    runtimeFault.printStackTrace();
                }

                vSphere.FolderMap.putIfAbsent(meKey, (Folder)me);
                vSphere.FolderMewMap.putIfAbsent(meKey, this);
                break;
            case "Datacenter":
                meType = Type.DATACENTER;
                break;
            case "HostSystem":
                meType = Type.HOSTSYSTEM;
                break;
            case "DistributedVirtualPortgroup":
                vmsOfThisNetwork = new HashSet<>();
                meType = Type.NETWORK;
                break;
            default:
                meType = Type.UNKNOWN;
                break;
        }
    }

    public String getName() {
        return name;
    }

    public ManagedEntityWrapper duplicate(boolean useFullPath) {
        return duplicateSelf(useFullPath);
    }

    private ManagedEntityWrapper duplicateSelf(boolean useFullPath) {
        ManagedEntityWrapper mewDup = new ManagedEntityWrapper(null, useFullPath);
        mewDup.meChildrenKey = this.meChildrenKey;
        mewDup.isNetworked = this.isNetworked;
        mewDup.type = this.type;
        mewDup.name = this.name;
        mewDup.meType = this.meType;
        mewDup.fullPath = useFullPath;
        mewDup.meKey = this.meKey;
        mewDup.meParentKey = this.meParentKey;
        mewDup.nets = this.nets;
        mewDup.path = this.path;

        return mewDup;
    }

    /**
     * If ManagedEntity is a VM this function will restart it
     * If ManagedEntity is a Folder this function will restart all VMs recursively
     */
    public void startVM() {
        if(meType == Type.VM) {
            String error = "";
            boolean success;
            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            try {
                vm.powerOnVM_Task(null);
                success = true;
            } catch (RemoteException ex1) {
                error = ex1.toString();
                try {
                    vm.rebootGuest();
                    success = true;
                } catch (RemoteException ex2) {
                    success = false;
                    error = ex2.toString();
                }
            }

            if(success) {
                MainGUI.getInsatnce().setStatusMessage("Status: " + getName() + " is now running");
            } else {
                MainGUI.getInsatnce().setStatusMessage("Status: Failed to start " + getName());
                GuiHelper.messageBox(error, "Failed to start " + getName(), true);
            }
        } else if(meType == Type.FOLDER) {
            ManagedEntityWrapper[] arr = getFolderChildren();
            for (ManagedEntityWrapper mew : arr) {
                mew.startVM();
            }
        }
    }

    /**
     * If ManagedEntity is a VM this function will shutdown it
     * If ManagedEntity is a Folder this function will shutdown all VMs recursively
     */
    public void shutdownVM() {
        if(meType == Type.VM) {
            String error = "";
            boolean success;
            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);

            try {
                vm.powerOffVM_Task();
                success = true;
            } catch (RemoteException ex1) {
                error = ex1.toString();
                try {
                    vm.shutdownGuest();
                    success = true;
                } catch (RemoteException ex2) {
                    success = false;
                    error = ex2.toString();
                }
            }

            if(success) {
                MainGUI.getInsatnce().setStatusMessage("Status: " + getName() + " is now stopped");
            } else {
                MainGUI.getInsatnce().setStatusMessage("Status: Failed to shutdown " + getName());
                GuiHelper.messageBox(error, "Failed to shutdown " + getName(), true);
            }
        } else if(meType == Type.FOLDER) {
            ManagedEntityWrapper[] arr = getFolderChildren();
            for(int i=0; i<arr.length; i++) {
                ManagedEntityWrapper mew = arr[i];
                if(mew != null)
                    mew.shutdownVM();
            }
        }
    }

    /**
     * If ManagedEntity is a VM this function will snapshot it.
     * If ManagedEntity is a Folder this function will snapshot all VMs recursively
     */
    public void snapshotVM(String name, String description) {
        if(meType == Type.VM) {
            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            try {
                vm.createSnapshot_Task(name, description, false, true);
                MainGUI.getInsatnce().setStatusMessage("Status: Snapshot for " + getName() + " has been created");
            } catch (RemoteException ex) {
                MainGUI.getInsatnce().setStatusMessage("Status: Failed to snapshot " + getName());
                GuiHelper.messageBox(ex.toString(), "Failed to snapshot " + getName(), true);
            }

        } else if(meType == Type.FOLDER) {
            ManagedEntityWrapper[] arr = getFolderChildren();
            for (ManagedEntityWrapper mew : arr) {
                mew.snapshotVM(name, description);
            }
        }
    }

    /**
     * If ManagedEntity is a VM this function will return true if it is running
     */
    public boolean isVMPoweredOn() {
        if(meType == Type.VM) {
            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            if(vm == null) return false;
            VirtualMachineRuntimeInfo vmRuntime = vm.getRuntime();
            if(vmRuntime == null) return false;

            return vmRuntime.getPowerState().equals(VirtualMachinePowerState.poweredOn);
        }

        return false;
    }

    /**
     * If ManagedEntity is a VM this function will return it's guest name
     */
    public String getVMGuestName() {
        if(meType == Type.VM) {
            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            return vm.getConfig().guestFullName;
        }

        return "";
    }

    /**
     * If ManagedEntity is a Folder this function will return it's children
     * @return child items
     */
    public ManagedEntityWrapper[] getFolderChildren() {
        if(meType == Type.FOLDER) {
            ManagedEntityWrapper[] ret = new ManagedEntityWrapper[meChildrenKey.size()];
            Iterator<String> it = meChildrenKey.iterator();
            int i = 0;
            while(it.hasNext()) {
                ManagedEntityWrapper mewAtI = null;
                String key = it.next();

                if(vSphere.FolderMewMap.containsKey(key))
                    mewAtI = vSphere.FolderMewMap.get(key);
                else if(vSphere.VirtualMachinesMewMap.containsKey(key))
                    mewAtI = vSphere.VirtualMachinesMewMap.get(key);

                ret[i] = mewAtI;
                i++;
            }

            return ret;
        }

        return null;
    }

    /*public Folder getDatacenterChildren() {
        if(meType == Type.DATACENTER) {
            try {
                return ((Datacenter)me).getVmFolder();
            } catch (RemoteException ex) {
                MainGUI.getInsatnce().setStatusMessage("Status: Failed to retrieve child items of " + getName());
                GuiHelper.messageBox(ex.toString(), "Failed to retrieve child items of " + getName(), true);
            }
        }

        return null;
    }*/

    /**
     * If ManagedEntity is a Template this function will create a VM from the Template with the same name
     * @param parentFolder target folder
     * @return true on success, false otherwise
     * @throws RemoteException
     */
    public boolean cloneVM(ManagedEntityWrapper parentFolder) throws RemoteException {
        if(meType == Type.TEMPLATE) {
            ArrayList<ResourcePool> resourcePools = vSphere.getInstance().getResourcePools();
            if(resourcePools == null || resourcePools.size() == 0)  {
                System.out.println("No resource pools found!!");
                return false;
            }

            VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
            VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
            //cloneSpec.template = false;

            //TODO: allow user to select resource pool
            relocateSpec.setPool(resourcePools.get(0).getMOR());
            cloneSpec.setLocation(relocateSpec);
            cloneSpec.setPowerOn(false);
            cloneSpec.setTemplate(false);

            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            Folder folder = vSphere.FolderMap.get(parentFolder.meKey);
            Task task = vm.cloneVM_Task(folder, getName(), cloneSpec);

            String status = "";
            try {
                status = task.waitForTask();
            } catch(InterruptedException iex) {
                System.out.println("Waiting for task got interrupted");
                iex.printStackTrace();
            }

            //System.out.println("Source vm: " + vm.toString());

            return status.equals(Task.SUCCESS);

        }

        return false;
    }

    public void getNicInfo() {
        if(meType == Type.VM) {
            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            VirtualMachineConfigInfo configInfo = vm.getConfig();

            // configInfo.getHardware().getDevice().length == 0 -> ethernet card does not exist
            for(VirtualDevice device : configInfo.getHardware().getDevice()) {
                if (device instanceof VirtualEthernetCard) {
                    VirtualEthernetCard vec = (VirtualEthernetCard) device;
                    //CustomizationAdapterMapping mapping = new CustomizationAdapterMapping();
                    //CustomizationIPSettings settings = new CustomizationIPSettings();
                    System.out.println("Device label: " + vec.getDeviceInfo().label);
                    System.out.println("Address type: " + vec.addressType);
                    System.out.println("MAC address: " + vec.macAddress);
                    System.out.println("Device summary: " + vec.getDeviceInfo().getSummary()); // Can be compare to UUID

                    VirtualDeviceBackingInfo vecnbi = vec.getBacking();
                    if(vecnbi != null) {
                        //VirtualEthernetCardLegacyNetworkBackingInfo
                        if(vecnbi instanceof VirtualEthernetCardDistributedVirtualPortBackingInfo) {
                            VirtualEthernetCardDistributedVirtualPortBackingInfo vecdvpb = (VirtualEthernetCardDistributedVirtualPortBackingInfo) vecnbi;
                            DistributedVirtualSwitchPortConnection dvspc = vecdvpb.getPort();

                            if (dvspc != null) {
                                System.out.println("Switch UUID: " + dvspc.switchUuid);
                                System.out.println("Port key: " + dvspc.getPortKey());
                                System.out.println("Port group: " + dvspc.portgroupKey);
                            }
                        } else if(vecnbi instanceof VirtualEthernetCardLegacyNetworkBackingInfo) { // Ethernet card not configured?
                            VirtualEthernetCardLegacyNetworkBackingInfo veclnbi = (VirtualEthernetCardLegacyNetworkBackingInfo) vecnbi;
                            System.out.println("Device name: " + veclnbi.deviceName);
                        }
                    }

                    System.out.println();
                }
            }
        }
    }

    /**
     * If ManagedEntityWrapper is a VM, this method will disconnect it from the specified network
     * @param networkName Name of the network
     * @return `true` on success, `false` otherwise
     */
    public boolean unAssignFromNetwork(String networkName) {
        if(meType == Type.VM) {
            System.out.println("Attempting to un assign `" + getName() + "` from network `" + networkName + "`");

            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            boolean success = false;

            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.remove);

            VirtualEthernetCard vec = getNicFromNetName(networkName);
            if(vec == null) {
                System.out.println("Failed to find Ethernet card with selected network name");
                return false;
            }
            nicSpec.setDevice(vec);

            System.out.println("Removing adapter: " + vec.getBacking().toString());

            vmConfigSpec.setDeviceChange(new VirtualDeviceConfigSpec []{nicSpec});
            String status = "";
            try {
                Task task = vm.reconfigVM_Task(vmConfigSpec);
                status = task.waitForTask();
                success = status.equals(Task.SUCCESS);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(success) {
                System.out.println("Successfully unassigned VM from selected network!");

                for(Network n : nets) { // remove us from all adaptors
                    vSphere.NetworkMewMap.get(n.toString()).vmsOfThisNetwork.remove(meKey);
                }

                try {
                    nets = vm.getNetworks();
                    isNetworked = nets != null && nets.length != 0;
                    ManagedEntityWrapper mewVMOrigin = vSphere.VirtualMachinesMewMap.get(meKey);
                    mewVMOrigin.isNetworked = isNetworked;
                    mewVMOrigin.nets = nets;
                    for(Network n : nets) { // make sure we are still visible for other adaptors
                        vSphere.NetworkMewMap.get(n.toString()).vmsOfThisNetwork.add(meKey);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            return success;
        }

        return false;
    }

    /**
     * If ManagedEntityWrapper is a VM, this method will connect it to the specified network
     * @param networkName Name of the network
     * @return `true` on success and `false` otherwise
     */
    public boolean assignToNetwork(String networkName) {
        //VirtualEthernetCard vec = getNicFromNetName(networkName);
        if(meType == Type.VM) {
            System.out.println("Attempting to assign `" + getName() + "` to network `" + networkName + "`");
            MainGUI.getInsatnce().setStatusMessage("Status: Attempting to assign `" + getName() + "` to network `" + networkName + "`");
            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            VirtualMachineConfigInfo configInfo = vm.getConfig();

            // case 1: if network adapter does not exist - create a new one and assign it to target network
            // case 2: if network adapter exists, but is not configured - configure it to target network
            // case 3: if network adapter exists and is configured - create a new one and assign it to target network

            boolean case2 = false;
            boolean success = false;
            VirtualEthernetCardDistributedVirtualPortBackingInfo nicBacking = creteNewBacking(networkName);
            if(nicBacking == null) {
                System.out.println("Failed to generate backing...");
                return false;
            }
            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();

            for(VirtualDevice device : configInfo.getHardware().getDevice()) {
                if (device instanceof VirtualEthernetCard) {
                    VirtualEthernetCard vec = (VirtualEthernetCard) device;
                    VirtualDeviceBackingInfo vecnbi = vec.getBacking();
                    if(vecnbi == null) continue;

                    if(vecnbi instanceof VirtualEthernetCardLegacyNetworkBackingInfo) { // case 2 - Ethernet card not configured
                        case2 = true;
                        System.out.println("Found unused network card, utilizing it...");

                        nicSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);
                        vec.setBacking(nicBacking);
                        nicSpec.setDevice(vec);
                    }
                }
            }

            if(!case2) {
                System.out.println("No unused network found, creating a new one...");
                // case 1 - Ethernet card not found
                // case 3 - No Ethernet cards available
                // Create a new one

                nicSpec.setOperation(VirtualDeviceConfigSpecOperation.add);

                VirtualEthernetCard nic = new VirtualPCNet32();
                //nic.setAddressType("generated");
                nic.setBacking(nicBacking);
                //nic.setKey(4);
                nicSpec.setDevice(nic);
            }

            vmConfigSpec.setDeviceChange(new VirtualDeviceConfigSpec []{nicSpec});
            String status = "";
            try {
                Task task = vm.reconfigVM_Task(vmConfigSpec);
                status = task.waitForTask();
                success = status.equals(Task.SUCCESS);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(success) {
                System.out.println("Successfully assigned VM to selected network!");
                MainGUI.getInsatnce().setStatusMessage("Status: Successfully assigned VM to selected network!");
                try {
                    nets = vm.getNetworks();
                    isNetworked = nets != null && nets.length != 0;
                    ManagedEntityWrapper mewVMOrigin = vSphere.VirtualMachinesMewMap.get(meKey);
                    mewVMOrigin.isNetworked = isNetworked;
                    mewVMOrigin.nets = nets;
                    for(Network n : nets) {
                        vSphere.NetworkMewMap.get(n.toString()).vmsOfThisNetwork.add(meKey);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            return success;
        }

        return false;
    }


    /**
     * Will generate a new Backing Information for specified network
     * @param networkName Name of the network
     * @return
     */
    private VirtualEthernetCardDistributedVirtualPortBackingInfo creteNewBacking(String networkName) {
        try {
            VirtualEthernetCardDistributedVirtualPortBackingInfo nicBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
            DistributedVirtualPortgroup dvp = vSphere.getInstance().searchForPortgroup(networkName);
            nicBacking.setPort(new DistributedVirtualSwitchPortConnection());
            nicBacking.getPort().setPortgroupKey(dvp.getConfig().key);
            nicBacking.getPort().setSwitchUuid(vSphere.mainSwitchUUID);

            return nicBacking;
        } catch (RemoteException e) {
            e.printStackTrace();
            GuiHelper.messageBox(e.toString(), "Failed to create new network backing for selected network", true);
        }

        return null;
    }

    /**
     * Will return Distributed Virtual Switch Port Connection if there is already at least 1 VM connected to that network
     * @param networkName Name of the network
     * @return
     */
    private DistributedVirtualSwitchPortConnection getSwitchFromNetName(String networkName) {
        try {
            DistributedVirtualPortgroup dvp = vSphere.getInstance().searchForPortgroup(networkName);

            for(VirtualMachine vm : dvp.getVms()) {
                ManagedEntityWrapper mewVM = vSphere.VirtualMachinesMewMap.get(vm.toString());
                VirtualEthernetCard vec = mewVM.getNicFromNetName(networkName);

                if(vec != null && vec.getBacking() instanceof VirtualEthernetCardDistributedVirtualPortBackingInfo) {
                    VirtualEthernetCardDistributedVirtualPortBackingInfo vecdvpb = (VirtualEthernetCardDistributedVirtualPortBackingInfo) vec.getBacking();
                    DistributedVirtualSwitchPortConnection dvspc = vecdvpb.getPort();

                    if(dvspc != null) {
                        //System.out.println("Port group key: " + dvspc.portgroupKey);
                        //System.out.println("Port key: " + dvspc.portKey);
                        //System.out.println("Switch UUID: " + dvspc.switchUuid);

                        return dvspc;
                    }
                }
            }

        } catch (RemoteException e) {
            e.printStackTrace();
            GuiHelper.messageBox(e.toString(), "Failed to find selected network's switch", true);
        }

        return null;
    }

    /**
     * If ManagedEntityWrapper is a VM, this will return VirtualEthernetCard that is connected to the provided network
     * @param networkName Name of the network
     * @return
     */
    public VirtualEthernetCard getNicFromNetName(String networkName) {
        if(meType == Type.VM) {
            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            VirtualMachineConfigInfo configInfo = vm.getConfig();

            for(VirtualDevice device : configInfo.getHardware().getDevice()) {
                if (device instanceof VirtualEthernetCard) {
                    VirtualEthernetCard vec = (VirtualEthernetCard) device;
                    VirtualDeviceBackingInfo vecnbi = vec.getBacking();
                    if(vecnbi == null) continue;

                    if(!(vecnbi instanceof VirtualEthernetCardDistributedVirtualPortBackingInfo)) return null;

                    VirtualEthernetCardDistributedVirtualPortBackingInfo vecdvpb = (VirtualEthernetCardDistributedVirtualPortBackingInfo) vecnbi;
                    DistributedVirtualSwitchPortConnection dvspc = vecdvpb.getPort();
                    if(dvspc == null) continue;

                    String portgroup = dvspc.portgroupKey;
                    if(portgroup == null) continue;

                    try {
                        DistributedVirtualPortgroup portgroupSearch = vSphere.getInstance().searchForPortgroup(networkName);
                        if(portgroupSearch != null && portgroupSearch.getKey().equals(portgroup))
                            return vec;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return null;
    }

    /**
     * If ManagedEntityWrapper is a VM, tis method will remove all network adapters from it
     */
    public void removeAllNetworkAdapters() {
        if(meType == Type.VM) {
            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            VirtualMachineConfigInfo configInfo = vm.getConfig();

            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.remove);

            for(VirtualDevice device : configInfo.getHardware().getDevice()) {
                if (device instanceof VirtualEthernetCard) {
                    nicSpec.setDevice(device);
                }
            }

            vmConfigSpec.setDeviceChange(new VirtualDeviceConfigSpec []{nicSpec});
            String status = "";
            try {
                Task task = vm.reconfigVM_Task(vmConfigSpec);
                status = task.waitForTask();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * If ManagedEntity is a VM this function will return first network it is connected to
     * @return Network VM is connected to or null if ManagedEntity is not a VM or it is not connected to a network
     */
    public Network getFirstNetwork() {
        if(meType == Type.VM) {
            VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
            if(vm == null) {
                System.out.println("VM with hash: " + meKey + " does not have any networks?");
                return null;
            }

            return isNetworked ? nets[0] : null;
        }

        return null;
    }

    /**
     * If Managed Entity is a VM this function will return name of the first network it is connected to
     * @return Name of the network VM is connected to or "Not Networked" if ManagedEntity is not a VM or it is not connected to a network
     */
    public String getFirstNetworkName() {
        Network n = this.getFirstNetwork();
        if(n == null) return "Not Networked";

        return n.getName();
    }

    public Type type() {
        return meType;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        if(meType != Type.VM || !fullPath) return getName();

        if(path == null) {
            // Obtain full path
            ManagedEntityWrapper temp_me = this;
            StringBuilder result = new StringBuilder("");

            while (temp_me != null && !temp_me.getName().equals("vm")) {
                result.insert(0, "/" + temp_me.getName());
                temp_me = vSphere.FolderMewMap.getOrDefault(temp_me.meParentKey, null);
            }
            path = result.toString();
        }

        return path;
    }

    public enum Type {
        TEMPLATE, VM, FOLDER, DATACENTER, HOSTSYSTEM, NETWORK, UNKNOWN
    }

    @Nullable
    public int compareTo(ManagedEntityWrapper second) {
        if(second == null) return 1;

        return this.toString().compareTo(second.toString());
    }
}