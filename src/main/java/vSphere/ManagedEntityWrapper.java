package vSphere;

import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.*;
import gui.GuiHelper;
import gui.MainWindow.MainGUI;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import java.rmi.RemoteException;
import java.util.TreeSet;

public class ManagedEntityWrapper implements Comparable<ManagedEntityWrapper> {
    private ManagedEntity me;
    private String type;
    private Type meType;

    /**
     * Constructor will figure out what type of ManagedEntity it is and handle it appropriately
     * @param me ManagedEntity we want to wrap
     */
    public ManagedEntityWrapper(ManagedEntity me) {
        type = me.getMOR().type;
        this.me = me;

        switch (type) {
            case "VirtualMachine":
                VirtualMachineConfigInfo vmci = ((VirtualMachine) me).getConfig();
                if (vmci.template) {
                    type += " (Template)";
                    meType = Type.TEMPLATE;
                } else {
                    meType = Type.VM;
                }
                break;
            case "Folder":
                meType = Type.FOLDER;
                break;
            case "Datacenter":
                meType = Type.DATACENTER;
                break;
            default:
                meType = Type.UNKNOWN;
                break;
        }
    }

    public String getName() {
        return me.getName();
    }

    /**
     * If ManagedEntity is a VM this function will restart it
     * If ManagedEntity is a Folder this function will restart all VMs recursively
     */
    public void startVM() {
        if(meType == Type.VM) {
            String error = "";
            boolean success;
            try {
                ((VirtualMachine) me).powerOnVM_Task(null);
                success = true;
            } catch (RemoteException ex1) {
                error = ex1.toString();
                try {
                    ((VirtualMachine) me).rebootGuest();
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

            try {
                ((VirtualMachine) me).powerOffVM_Task();
                success = true;
            } catch (RemoteException ex1) {
                error = ex1.toString();
                try {
                    ((VirtualMachine) me).shutdownGuest();
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
            for (ManagedEntityWrapper mew : arr) {
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
            try {
                ((VirtualMachine) me).createSnapshot_Task(name, description, false, true);
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
            return ((VirtualMachine) me).getRuntime().getPowerState().equals(VirtualMachinePowerState.poweredOn);
        }

        return false;
    }

    /**
     * If ManagedEntity is a VM this function will return it's guest name
     */
    public String getVMGuestName() {
        if(meType == Type.VM) {
            return ((VirtualMachine) me).getConfig().guestFullName;
        }

        return "";
    }

    /**
     * If ManagedEntity is a Folder this function will return it's children
     * @return child items
     */
    public ManagedEntityWrapper[] getFolderChildren() {
        if(meType == Type.FOLDER) {
            try {
                ManagedEntity[] mes = ((Folder)me).getChildEntity();
                ManagedEntityWrapper[] ret = new ManagedEntityWrapper[mes.length];
                for(int i=0; i<mes.length; i++) {
                    ret[i] = new ManagedEntityWrapper(mes[i]);
                }

                return ret;
            } catch (RemoteException ex) {
                MainGUI.getInsatnce().setStatusMessage("Failed to retrieve child items of " + getName());
                GuiHelper.messageBox(ex.toString(), "Failed to retrieve child items of " + getName(), true);
            }
        }

        return null;
    }

    public Folder getDatacenterChildren() {
        if(meType == Type.DATACENTER) {
            try {
                return ((Datacenter)me).getVmFolder();
            } catch (RemoteException ex) {
                MainGUI.getInsatnce().setStatusMessage("Failed to retrieve child items of " + getName());
                GuiHelper.messageBox(ex.toString(), "Failed to retrieve child items of " + getName(), true);
            }
        }

        return null;
    }

    /* Not tested yet
    public boolean cloneVM(VirtualMachine vm, String clonedVMName) throws RemoteException {
        if(vm == null) {
            return false;
        }

        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        cloneSpec.setLocation(new VirtualMachineRelocateSpec());
        cloneSpec.setPowerOn(false);
        cloneSpec.setTemplate(false);

        Task task = vm.cloneVM_Task((Folder) vm.getParent(), clonedVMName, cloneSpec);

        String status = task.waitForMe();

        if(status != Task.SUCCESS) {
            return false;
        }

        //vmCount++;

        return true;
    } */

    public Type type() {
        return meType;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        return getName();
    }

    public enum Type {
        TEMPLATE, VM, FOLDER, DATACENTER, UNKNOWN
    }

    @Nullable
    public int compareTo(ManagedEntityWrapper second) {
        if(second == null) return 1;

        return this.toString().compareTo(second.toString());
    }
}