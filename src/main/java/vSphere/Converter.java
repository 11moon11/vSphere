package vSphere;

//import com.google.common.collect.*;
//import com.google.common.base.*;
import com.vmware.vim25.mo.*;
//import com.vmware.vim25.*;
//import java.util.*;

public class Converter {
    public static Folder convert(ManagedEntity me) {
        return (Folder)me;
    }

    /**
     * Converts an array of ManagedEntity to Folder array
     * @param me ManagedEntity array
     * @return Folder array
     */
    public static Folder[] convert(ManagedEntity[] me) {
        /* Alternative method (slow)
        Iterable<Folder> folders = Iterables.transform(Arrays.asList(me), new Function<ManagedEntity, Folder>() {
            public Folder apply(ManagedEntity input) {
                return (Folder)input;
            }
        });
        return (Folder[])Arrays.asList(folders).toArray();
        */
        if(me == null || me.length == 0) {
            return null;
        }

        Folder[] folders = new Folder[me.length];
        for(int i=0; i<me.length; i++) {
            folders[i] = convert(me[i]);
        }

        return folders;
    }
}
