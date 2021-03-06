/* Copyright (c) 2010 EugineLev, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */
package com.sun.jna.platform.win32;

import java.util.List;
import java.util.LinkedList;

import junit.framework.TestCase;

import com.sun.jna.platform.win32.Winsvc.SC_ACTION;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS_PROCESS;
import com.sun.jna.platform.win32.Winsvc.SERVICE_FAILURE_ACTIONS;

public class W32ServiceTest extends TestCase {

    private final W32ServiceManager _serviceManager = new W32ServiceManager();

    @Override
    public void setUp() {
        _serviceManager.open(Winsvc.SC_MANAGER_CONNECT);
    }

    @Override
    public void tearDown() {
        _serviceManager.close();
    }
    
    public void testCreateServiceDeleteService() {
        // This tests:
        // - com.sun.jna.platform.win32.Advapi32.CreateService
        // - com.sun.jna.platform.win32.Advapi32.DeleteService
        // - com.sun.jna.platform.win32.Advapi32.SERVICE_DESCRIPTION
        Win32ServiceDemo.uninstall();
        assertTrue(Win32ServiceDemo.install());
        assertTrue(Win32ServiceDemo.uninstall());
    }

    public void testControlService() {
        // Cleanup in case of an unsuccessful previous run
        Win32ServiceDemo.uninstall();
        Win32ServiceDemo.install();
        // This test implicitly tests the "service side" functions/members:
        // - com.sun.jna.platform.win32.Advapi32.StartServiceCtrlDispatcher
        // - com.sun.jna.platform.win32.Advapi32.SERVICE_TABLE_ENTRY
        // - com.sun.jna.platform.win32.Advapi32.RegisterServiceCtrlHandlerEx
        // - com.sun.jna.platform.win32.Advapi32.SetServiceStatus
        // - com.sun.jna.platform.win32.Advapi32.SERVICE_MAIN_FUNCTION
        // - com.sun.jna.platform.win32.Advapi32.HandlerEx
        // - com.sun.jna.platform.win32.Advapi32.SERVICE_STATUS_HANDLE
        W32Service service = _serviceManager.openService(Win32ServiceDemo.serviceName, Winsvc.SERVICE_ALL_ACCESS);
        service.startService();
        assertEquals(service.queryStatus().dwCurrentState, Winsvc.SERVICE_RUNNING);
        service.pauseService();
        assertEquals(service.queryStatus().dwCurrentState, Winsvc.SERVICE_PAUSED);
        service.continueService();
        assertEquals(service.queryStatus().dwCurrentState, Winsvc.SERVICE_RUNNING);
        service.stopService();
        assertEquals(service.queryStatus().dwCurrentState, Winsvc.SERVICE_STOPPED);
        service.close();
        Win32ServiceDemo.uninstall();
    }

    public void testQueryStatus() {
        W32Service service = _serviceManager.openService("eventlog", Winsvc.SERVICE_QUERY_STATUS);
        SERVICE_STATUS_PROCESS status = service.queryStatus();
        assertTrue(status.dwCurrentState == Winsvc.SERVICE_RUNNING
                || status.dwCurrentState == Winsvc.SERVICE_STOPPED);
        service.close();
    }

    public void testSetAndGetFailureActions() {
        final String svcId = "w32time";
        final String rebootMsg = "Restarting " + svcId + " due to service failure";
        final String command = "echo " + svcId + " failure";
        final int resetPeriod = 5000;

        W32Service service = _serviceManager.openService(svcId, Winsvc.SC_MANAGER_ALL_ACCESS);
        SERVICE_FAILURE_ACTIONS prevActions = service.getFailureActions();

        List<SC_ACTION> actions = new LinkedList<SC_ACTION>();

        SC_ACTION action = new SC_ACTION();
        action.type = Winsvc.SC_ACTION_RESTART;
        action.delay = 1000;
        actions.add(action);

        action = new SC_ACTION();
        action.type = Winsvc.SC_ACTION_REBOOT;
        action.delay = 2000;
        actions.add(action);

        action = new SC_ACTION();
        action.type = Winsvc.SC_ACTION_RUN_COMMAND;
        action.delay = 3000;
        actions.add(action);

        action = new SC_ACTION();
        action.type = Winsvc.SC_ACTION_NONE;
        action.delay = 4000;
        actions.add(action);

        service.setFailureActions(actions, resetPeriod, rebootMsg, command);

        SERVICE_FAILURE_ACTIONS changedActions = service.getFailureActions();
        assertEquals(changedActions.lpRebootMsg, rebootMsg);
        assertEquals(changedActions.lpCommand, command);
        assertEquals(changedActions.dwResetPeriod, resetPeriod);
        assertEquals(changedActions.cActions, 4);
        SC_ACTION[] actionArray = (SC_ACTION[]) changedActions.lpsaActions.toArray(changedActions.cActions);
        assertEquals(actionArray[0].type, Winsvc.SC_ACTION_RESTART);
        assertEquals(actionArray[0].delay, 1000);
        assertEquals(actionArray[1].type, Winsvc.SC_ACTION_REBOOT);
        assertEquals(actionArray[1].delay, 2000);
        assertEquals(actionArray[2].type, Winsvc.SC_ACTION_RUN_COMMAND);
        assertEquals(actionArray[2].delay, 3000);
        assertEquals(actionArray[3].type, Winsvc.SC_ACTION_NONE);
        assertEquals(actionArray[3].delay, 4000);

        // restore old settings
        Advapi32.INSTANCE.ChangeServiceConfig2(service._handle, Winsvc.SERVICE_CONFIG_FAILURE_ACTIONS,
                prevActions);

        service.close();
    }

    public void testSetFailureActionsFlag() {
        W32Service service = _serviceManager.openService("eventlog", Winsvc.SC_MANAGER_ALL_ACCESS);
        boolean prevFlag = service.getFailureActionsFlag();
        service.setFailureActionsFlag(!prevFlag);
        assertTrue(prevFlag != service.getFailureActionsFlag());
        service.setFailureActionsFlag(prevFlag);
        service.close();
    }
}
