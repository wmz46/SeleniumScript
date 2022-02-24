package com.iceolive.selenium;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wangmianzhe
 */
public class Win32Api {
    /**
     * 根据标题获取窗口句柄
     * @param title
     * @return
     */
    public static long getByTitle(String title) {
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, title);
        return Pointer.nativeValue(hwnd.getPointer());
    }

    /**
     * 获取窗口句柄
     *
     * @param processId 进程pid
     * @return
     */
    public static List<Long> getAllByPID(int processId) {
        Pointer pointer = new Memory(5);
        pointer.setInt(0, processId);
        WndEnumProc wndEnumProc = new WndEnumProc();
        User32.INSTANCE.EnumWindows(wndEnumProc, pointer);
        return wndEnumProc.hwnds;
    }

    /**
     * 获取子控件句柄
     *
     * @param hwnd
     * @return
     */
    public static List<Long> getChildren(long hwnd) {
        WndEnumProc wndEnumProc = new WndEnumProc();
        User32.INSTANCE.EnumChildWindows(new WinDef.HWND(new Pointer(hwnd)), wndEnumProc, Pointer.createConstant(0));
        return wndEnumProc.hwnds;
    }

    /**
     * 获取窗口或控件标题
     *
     * @param hwnd 窗口或控件句柄
     * @return
     */
    public static String getTitle(long hwnd) {
        WinDef.HWND hwnd1 = new WinDef.HWND(new Pointer(hwnd));
        int length = User32.INSTANCE.GetWindowTextLength(hwnd1);
        char[] windowText = new char[length + 1];
        User32.INSTANCE.GetWindowText(hwnd1, windowText, windowText.length);
        String wText = Native.toString(windowText);
        return wText;
    }

    /**
     * 窗口始终置顶
     *
     * @param hwnd
     */
    public static void setTopMost(long hwnd) {
        WinDef.HWND hwnd1 = new WinDef.HWND(new Pointer(hwnd));
        User32.INSTANCE.SetWindowPos(hwnd1, new WinDef.HWND(new Pointer(-1)), 0, 0, 0, 0, 1 | 2);
    }

    /**
     * 控制窗口大小
     *
     * @param hwnd 窗口句柄
     * @param i    1 正常 2 最小化 3 最大化
     */
    public static void showWindow(long hwnd, int i) {
        WinDef.HWND hwnd1 = new WinDef.HWND(new Pointer(hwnd));
        User32.INSTANCE.ShowWindow(hwnd1, i);
    }

    /**
     * 获取进程pid
     *
     * @param hwnd 窗口句柄
     * @return
     */

    public static int getPID(long hwnd) {
        WinDef.HWND hwnd1 = new WinDef.HWND(new Pointer(hwnd));
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd1, pid);
        return pid.getValue();
    }

    /**
     * 截图
     * @param hwnd
     * @return
     */
    public static BufferedImage getScreenShot(long hwnd){
        WinDef.HWND hwnd1 = new WinDef.HWND(new Pointer(hwnd));
        return  GDI32Util.getScreenshot(hwnd1);
    }

    /**
     * 获取桌面句柄
     * @return
     */
    public static long getDesktop(){
        return Pointer.nativeValue(User32.INSTANCE.GetDesktopWindow().getPointer());
    }
    public static class WndEnumProc implements WinUser.WNDENUMPROC {
        private final List<Long> hwnds = new ArrayList<>();

        @Override
        public boolean callback(WinDef.HWND hwnd, Pointer pointer) {

            int processId = getPID(Pointer.nativeValue(hwnd.getPointer()));
            if (pointer != null) {
                if (processId == pointer.getInt(0)) {
                    hwnds.add(Pointer.nativeValue(hwnd.getPointer()));
                }
            } else {
                hwnds.add(Pointer.nativeValue(hwnd.getPointer()));
            }
            return true;

        }




    }


}

