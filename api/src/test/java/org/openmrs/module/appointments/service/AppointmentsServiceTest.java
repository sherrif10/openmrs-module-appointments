package org.openmrs.module.appointments.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;

import org.openmrs.module.appointments.dao.AppointmentAuditDao;
import org.openmrs.module.appointments.model.*;
import org.openmrs.module.appointments.util.DateUtil;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class AppointmentsServiceTest extends BaseModuleWebContextSensitiveTest {
    private String adminUser;
    private String adminUserPassword;
    private String manageUser;
    private String manageUserPassword;
    private String readOnlyUser;
    private String readOnlyUserPassword;
    private String noPrivilegeUser;
    private String noPrivilegeUserPassword;

    @Autowired
    AppointmentsService appointmentsService;
    @Autowired
    ProviderService providerService;
    @Autowired
    PatientService patientService;

    @Mock
    private AppointmentAuditDao appointmentAuditDao;

    @Autowired
    AppointmentServiceDefinitionService appointmentServiceDefinitionService;

    @Before
    public void init() throws Exception {
        adminUser = "super-user";
        adminUserPassword = "P@ssw0rd";
        manageUser = "manage-user";
        manageUserPassword = "P@ssw0rd";
        readOnlyUser = "read-only-user";
        readOnlyUserPassword = "P@ssw0rd";
        noPrivilegeUser = "no-privilege-user";
        noPrivilegeUserPassword = "P@ssw0rd";
        executeDataSet("userRolesandPrivileges.xml");
        executeDataSet("appointmentTestData.xml");
    }

    @Test
    public void shouldSaveAppointmentsOnlyIfUserHasManagePrivilege() throws Exception {
        Context.authenticate(manageUser, manageUserPassword);
        Appointment appointment = new Appointment();
        appointment.setPatient(new Patient());
        appointment.setService(new AppointmentServiceDefinition());
        Date startDateTime = DateUtil.convertToDate("2108-08-15T10:00:00.0Z", DateUtil.DateFormatType.UTC);
        Date endDateTime = DateUtil.convertToDate("2108-08-15T10:30:00.0Z", DateUtil.DateFormatType.UTC);
        appointment.setStartDateTime(startDateTime);
        appointment.setEndDateTime(endDateTime);
        appointment.setAppointmentKind(AppointmentKind.Scheduled);

        Set<AppointmentProvider> appointmentProviders = new HashSet<>();


        AppointmentProvider appointmentProvider1 = new AppointmentProvider();
        appointmentProvider1.setAppointment(appointment);
        appointmentProvider1.setProvider(providerService.getProvider(2220));
        appointmentProvider1.setResponse(AppointmentProviderResponse.ACCEPTED);


        AppointmentProvider appointmentProvider2 = new AppointmentProvider();
        appointmentProvider2.setAppointment(appointment);
        appointmentProvider2.setProvider(providerService.getProvider(2220));
        appointmentProvider2.setResponse(AppointmentProviderResponse.AWAITING);

        appointmentProviders.add(appointmentProvider1);
        appointmentProviders.add(appointmentProvider2);

        appointment.setProviders(appointmentProviders);
        Appointment app = appointmentsService.validateAndSave(appointment);
        assertNotNull(app);
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotSaveAppointmentsIfUserHasNoPrivilege() throws Exception {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        assertNotNull(appointmentsService.validateAndSave(new Appointment()));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotSaveAppointmentIfUserHasReadOnlyPrivilege() throws Exception {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        assertNotNull(appointmentsService.validateAndSave(new Appointment()));
    }

    @Test
    public void shouldGetAllAppointmentsIfUserHasReadOnlyPriviliege() throws Exception {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        assertNotNull(appointmentsService.getAllAppointments(null));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAllAppointemntsIfUserDoesNotHaveAnyPrivilege() throws Exception {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        assertNotNull(appointmentsService.getAllAppointments(null));
    }

    @Test
    public void shouldBeAbleToSearchAppointmentsIfUserHasReadOnlyPrivilege() throws Exception {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        assertNotNull(appointmentsService.search(new Appointment()));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotBeAbleToSearchAppointmentsIfUserHasNoPrivilege() throws Exception {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        assertNotNull(appointmentsService.search(new Appointment()));
    }

    @Test
    public void shouldGetAllFutureAppointmentsIfuserHasReadOnlyPrivilege() throws Exception {
        Context.authenticate(manageUser, manageUserPassword);
        AppointmentServiceDefinition appointmentServiceDefinition = new AppointmentServiceDefinition();
        appointmentServiceDefinition.setId(1);
        assertNotNull(appointmentsService.getAllFutureAppointmentsForService(appointmentServiceDefinition));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAllFutureAppointmentsForServiceIfUserHasNoPrivilege() throws Exception {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        AppointmentServiceDefinition appointmentServiceDefinition = new AppointmentServiceDefinition();
        appointmentServiceDefinition.setId(1);
        assertNotNull(appointmentsService.getAllFutureAppointmentsForService(appointmentServiceDefinition));
    }

    @Test
    public void shouldGetAllFutureAppointmentsForServiceTypeIfUserHasReadOnlyPrivilege() throws Exception {
        Context.authenticate(adminUser, adminUserPassword);
        AppointmentServiceType appointmentServiceType = new AppointmentServiceType();
        appointmentServiceType.setId(1);
        assertNotNull(appointmentsService.getAllFutureAppointmentsForServiceType(appointmentServiceType));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAllFutureAppointmentsForServiceTypeIfUserHasNoPrivilege() throws Exception {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        AppointmentServiceType appointmentServiceType = new AppointmentServiceType();
        appointmentServiceType.setId(1);
        assertNotNull(appointmentsService.getAllFutureAppointmentsForServiceType(appointmentServiceType));
    }

    @Test
    public void shouldGetAppointmentsForServiceIfUserHasReadOnlyPrivilege() throws Exception {
        Context.authenticate(manageUser, manageUserPassword);
        AppointmentServiceDefinition appointmentServiceDefinition = new AppointmentServiceDefinition();
        appointmentServiceDefinition.setId(1);
        assertNotNull(appointmentsService.getAppointmentsForService(appointmentServiceDefinition, null, null, null));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAppointmentsForServiceIfUserHasNoPrivilege() throws Exception {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        AppointmentServiceDefinition appointmentServiceDefinition = new AppointmentServiceDefinition();
        appointmentServiceDefinition.setId(1);
        assertNotNull(appointmentsService.getAppointmentsForService(appointmentServiceDefinition, null, null, null));
    }

    @Test
    public void shouldGetAppointmentByUuidIfUserHasReadOnlyPrivilege() throws Exception {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        assertEquals(null, appointmentsService.getAppointmentByUuid("uuid"));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAppointmentByUuidIfUserHasNoPrivilege() throws Exception {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        assertEquals(null, appointmentsService.getAppointmentByUuid("uuid"));
    }

    @Test
    public void shouldBeAbleToChangeStatusIfUserHasManagePrivilege() throws Exception {
        Context.authenticate(manageUser, manageUserPassword);
        appointmentsService.changeStatus(new Appointment(), "Completed", null);
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotBeAbleToChangeStatusIfUserHasReadOnlyPrivilege() throws Exception {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        appointmentsService.changeStatus(new Appointment(), "Completed", null);
    }

    @Test
    public void shouldGetAllAppointmentsInDateRangeIfUserHasReadOnlyPrivilege() throws Exception {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        assertNotNull(appointmentsService.getAllAppointmentsInDateRange(null, null));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAllAppointmentsInDateRangeIfUserHasNoPrivilege() throws Exception {
        Context.authenticate(noPrivilegeUser, readOnlyUserPassword);
        assertNotNull(appointmentsService.getAllAppointmentsInDateRange(null, null));
    }

    @Test(expected = APIException.class)
    public void shouldBeAbleToUndoStatusChangeIfUserHasManagePrivilege() throws Exception {
        Context.authenticate(manageUser, manageUserPassword);
        Appointment appointment = new Appointment();
        appointment.setId(1);
        appointmentsService.undoStatusChange(appointment);
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotBeAbleToUndoStatusChangeIfUserHasReadOnlyPrivilege() throws Exception {
        Context.authenticate(readOnlyUser, manageUserPassword);
        Appointment appointment = new Appointment();
        appointment.setId(1);
        appointmentsService.undoStatusChange(appointment);
    }

    @Test
    public void shouldFetchAppointments() throws Exception {
        Context.authenticate(adminUser, adminUserPassword);
        List<Appointment> allAppointments = appointmentsService.getAllAppointments(DateUtil.convertToDate("2108-08-15T00:00:00.0Z", DateUtil.DateFormatType.UTC));
        List<Appointment> appointments = allAppointments.stream().filter(appointment ->
            appointment.getId().equals(2)
        ).collect(Collectors.toList());

        assertNotNull(appointments);
        assertEquals(1, appointments.size());
        List<AppointmentProvider> appointmentProviders = appointments.get(0).getProviders().stream().filter(
                appointmentProvider -> appointmentProvider.getProvider().getId() == 2220).collect(Collectors.toList());
        assertEquals(1, appointmentProviders.size());
        Provider provider = appointmentProviders.get(0).getProvider();
        assertEquals("System OpenMRS", provider.getName());


    }
}