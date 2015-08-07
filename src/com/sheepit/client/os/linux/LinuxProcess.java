package com.sheepit.client.os.linux;

import java.lang.reflect.Field;
import java.util.BitSet;

import com.sheepit.client.Utils;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.PointerByReference;

public class LinuxProcess {
    
    public static boolean setAffinity(int pid, long affinity) {
        BitSet bs = Utils.convertLongToBitset64(affinity);
        System.out.println("setAffinity for pid:"+pid+" affinity"+affinity);
        if (pid == -1)
            return false;
        if (bs.isEmpty())
            return false;
        try {
            byte[] buff = bs.toByteArray();
            int cpuSetSizeInBytes = buff.length;
            Memory cpusetArray = new Memory(cpuSetSizeInBytes);
            cpusetArray.write(0, buff, 0, buff.length);
            return CLibrary.INSTANCE.sched_setaffinity(pid, cpuSetSizeInBytes, new PointerByReference(cpusetArray)) == 0;
        } catch (Exception e) {
            System.err.println("setAffinity error for pid:"+pid+" affinity"+affinity + " " + e);
            return false;
        }
    }
    
    public static int getPid(Process process) {
        try {
            Class<?> ProcessImpl = process.getClass();
            Field field = ProcessImpl.getDeclaredField("pid");
            field.setAccessible(true);
            return field.getInt(process);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            return -1;
        }
    }   
    
    interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary)
                Native.loadLibrary("c", CLibrary.class);

        int sched_setaffinity(final int pid,
                              final int cpusetsize,
                              final PointerType cpuset);
    }
}
