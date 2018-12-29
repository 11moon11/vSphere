package vSphere;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import gui.GuiHelper;
import gui.MainWindow.MainGUI;

import javax.annotation.Nullable;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class ManagedEntityWrapper implements Comparable<ManagedEntityWrapper> {
    public ArrayList<String> meChildrenKey;
    public String meKey, meParentKey;
    //private ManagedEntity me;
    private boolean fullPath;
    private String type;
    private String name;
    private Type meType;


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
                break;
            case "Folder":
                meType = Type.FOLDER;
                meChildrenKey = new ArrayList<>();
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
                MainGUI.getInsatnce().setStatusMessage(getName() + " is now running");
            } else {
                MainGUI.getInsatnce().setStatusMessage("Failed to start " + getName());
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
                MainGUI.getInsatnce().setStatusMessage(getName() + " is now stopped");
            } else {
                MainGUI.getInsatnce().setStatusMessage("Failed to shutdown " + getName());
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
                MainGUI.getInsatnce().setStatusMessage("Snapshot for " + getName() + " has been created");
            } catch (RemoteException ex) {
                MainGUI.getInsatnce().setStatusMessage("Failed to snapshot " + getName());
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
            return vm.getRuntime().getPowerState().equals(VirtualMachinePowerState.poweredOn);
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
            for(int i = 0; i< meChildrenKey.size(); i++) {
                ManagedEntityWrapper mewAtI = null;
                if(vSphere.FolderMewMap.containsKey(meChildrenKey.get(i)))
                    mewAtI = vSphere.FolderMewMap.get(meChildrenKey.get(i));
                else if(vSphere.VirtualMachinesMewMap.containsKey(meChildrenKey.get(i)))
                    mewAtI = vSphere.VirtualMachinesMewMap.get(meChildrenKey.get(i));

                ret[i] = mewAtI;
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
                MainGUI.getInsatnce().setStatusMessage("Failed to retrieve child items of " + getName());
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

            System.out.println("Source vm: " + vm.toString());

            return status.equals(Task.SUCCESS);

        }

        return false;
    }

    /**
     * If ManagedEntity is a VM this function will return first network it is connected to
     * @return Network VM is connected to or null if ManagedEntity is not a VM or it is not connected to a network
     */
    public Network getFirstNetwork() {
        if(meType == Type.VM) {
            try {
                VirtualMachine vm = vSphere.VirtualMachinesMap.get(meKey);
                if(vm == null) {
                    System.out.println("VM with hash: " + meKey + " does not have any networks?");
                    return null;
                }
                Network[] nets = vm.getNetworks();
                if(nets == null || nets.length == 0) return null;
                return nets[0];
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
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

        ManagedEntity temp_me = vSphere.VirtualMachinesMap.get(meKey);
        StringBuilder result = new StringBuilder(getName());
        while(temp_me.getParent() != null && !temp_me.getParent().getName().equals("vm")) {
            result.insert(0, temp_me.getParent().getName() + "/");
            temp_me = temp_me.getParent();
        }

        return result.toString();
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