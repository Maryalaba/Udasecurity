package com.udacity.catpoint.securityService.service;

import com.udacity.catpoint.imageService.ImageService;
import com.udacity.catpoint.securityService.data.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityServiceTest {
    private AutoCloseable closeable;
    @InjectMocks
    private SecurityService service;

    @Mock
    private SecurityRepository repository;

    @Mock
    private ImageService imageServiceInstance;

    @Captor
    private ArgumentCaptor<AlarmStatus> alarmArgumentCaptor;

    @Mock
    private Sensor sensorInstance;

    @Mock
    private BufferedImage bufferedImage;

    @BeforeEach
    void initService() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    /**
     * Test 1
     * If alarm is armed and a sensor becomes activated, put the system into pending alarm status
     */
    @Test
    @DisplayName("If alarm is armed and a sensor becomes activated, put the system into pending alarm status")
    public void pendSystemIfAlarmIsArmedAndSensorActivated() {
        doReturn(ArmingStatus.ARMED_AWAY).when(repository).getArmingStatus();
        doReturn(AlarmStatus.NO_ALARM).when(repository).getAlarmStatus();
        doReturn(false).when(sensorInstance).getActive();
        service.changeSensorActivationStatus(sensorInstance, true);
        verify(repository, times(1)).setAlarmStatus(alarmArgumentCaptor.capture());
        AlarmStatus actual = alarmArgumentCaptor.getValue();
        AlarmStatus expected = AlarmStatus.PENDING_ALARM;
        assertEquals(expected, actual);
    }

    /**
     * Test 2
     * If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    @DisplayName("If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm")
    public void setAlarmStatusToAlarmIfAlarmIsArmedAndSensorActivatedAndSystemPending(ArmingStatus armingStatus) {
        doReturn(armingStatus).when(repository).getArmingStatus();
        doReturn(AlarmStatus.PENDING_ALARM).when(repository).getAlarmStatus();
        doReturn(false).when(sensorInstance).getActive();
        service.changeSensorActivationStatus(sensorInstance, true);
        verify(repository, times(1)).setAlarmStatus(alarmArgumentCaptor.capture());
        AlarmStatus actual = alarmArgumentCaptor.getValue();
        AlarmStatus expected = AlarmStatus.ALARM;
        assertEquals(expected, actual);
    }


    /**
     * Test 3
     * If pending alarm and all sensors are inactive, return to no alarm state
     */
    @Test
    @DisplayName(("If pending alarm and all sensors are inactive, return to no alarm state"))
    public void setAlarmStatusToNoAlarmIfPendingAlarmAndSensorsAreInactive() {
        doReturn(AlarmStatus.PENDING_ALARM).when(repository).getAlarmStatus();
        doReturn(true).when(sensorInstance).getActive();
        service.changeSensorActivationStatus(sensorInstance, false);
        verify(sensorInstance, times(1)).setActive(false);
        verify(repository, times(1)).setAlarmStatus(alarmArgumentCaptor.capture());
        AlarmStatus actual = alarmArgumentCaptor.getValue();
        AlarmStatus expected = AlarmStatus.NO_ALARM;
        assertEquals(expected, actual);
    }

    /**
     * Test 4
     * If alarm is active, change in sensor state should not affect the alarm state.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("If alarm is active, change in sensor state should not affect the alarm state.")
    public void sensorStateNotToChangeIfAlarmIsActive(boolean sensorStatus) {
        doReturn(AlarmStatus.ALARM).when(repository).getAlarmStatus();
        service.changeSensorActivationStatus(sensorInstance, sensorStatus);
        verify(repository, times(0)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * Test 5
     * If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
    @Test
    @DisplayName("If a sensor is activated while already active and the system is in pending state, change it to alarm state")
    public void changeSystemStateToAlarmIfSensorIsActivatedAndPending() {
        doReturn(AlarmStatus.PENDING_ALARM).when(repository).getAlarmStatus();
        doReturn(false).when(sensorInstance).getActive();
        service.changeSensorActivationStatus(sensorInstance, true);
        verify(sensorInstance, times(1)).setActive(true);
        verify(repository, times(1)).setAlarmStatus(alarmArgumentCaptor.capture());
        AlarmStatus actual = alarmArgumentCaptor.getValue();
        AlarmStatus expected = AlarmStatus.ALARM;
        assertEquals(expected, actual);
    }

    /**
     * Test 6
     * If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    @DisplayName("If a sensor is deactivated while already inactive, make no changes to the alarm state")
    public void alarmStatusNotToChangeIfSensorIsDeactivatedAndInactive(AlarmStatus alarmStatus) {
        doReturn(alarmStatus).when(repository).getAlarmStatus();
        service.changeSensorActivationStatus(sensorInstance, false);
        verify(repository, times(0)).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * Test 7
     * If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status
     */
    @Test
    @DisplayName("If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status")
    public void changeSystemToAlarmIfCatImageIsIdentifiedAndSystemIsArmedHome() {
        doReturn(true).when(imageServiceInstance).imageContainsCat(any(BufferedImage.class), anyFloat());
        doReturn(ArmingStatus.ARMED_HOME).when(repository).getArmingStatus();
        service.processImage(bufferedImage);
        verify(repository, times(1)).setAlarmStatus(alarmArgumentCaptor.capture());
        AlarmStatus actual = alarmArgumentCaptor.getValue();
        AlarmStatus expected = AlarmStatus.ALARM;
        assertEquals(expected, actual);
    }

    /**
     * Test 8
     * If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active
     */
    @Test
    @DisplayName("If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active")
    public void changeSystemToNoAlarmIfImageIdentifiedIsNotCatAndSensorsNotActive() {
        doReturn(false).when(imageServiceInstance).imageContainsCat(any(BufferedImage.class), anyFloat());
        service.processImage(bufferedImage);
        verify(repository, times(1)).setAlarmStatus(alarmArgumentCaptor.capture());
        AlarmStatus actual = alarmArgumentCaptor.getValue();
        AlarmStatus expected = AlarmStatus.NO_ALARM;
        assertEquals(expected, actual);
    }

    /**
     * Test 9
     * If the system is disarmed, set the status to no alarm
     */
    @Test
    @DisplayName("If the system is disarmed, set the status to no alarm")
    public void setStatusToNoAlarmIfSystemIsDisarmed() {
        service.setArmingStatus(ArmingStatus.DISARMED);
        verify(repository, times(1)).setAlarmStatus(alarmArgumentCaptor.capture());
        AlarmStatus actual = alarmArgumentCaptor.getValue();
        AlarmStatus expected = AlarmStatus.NO_ALARM;
        assertEquals(expected, actual);
    }

    /**
     * Test 10
     * If the system is armed, reset all sensors to inactive
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    @DisplayName("If the system is armed, reset all sensors to inactive")
    public void resetAllSensorsToInactiveIfSystemIsArmed(ArmingStatus status) {
        Set<Sensor> sensors = getSensors(true, 4);
        doReturn(sensors).when(repository).getSensors();
        service.setArmingStatus(status);
        verify(repository, times(1)).setArmingStatus(status);
        verify(repository, times(1)).getSensors();
        service.getSensors().forEach(sensor1 -> assertFalse(sensor1.getActive()));
    }

    private Set<Sensor> getSensors(boolean status, int number) {
        Set<Sensor> sensors = new HashSet<>();
        while (number > 0) {
            String randomString = UUID.randomUUID().toString();
            Sensor sensor = new Sensor(randomString, SensorType.WINDOW);
            sensor.setActive(status);
            sensors.add(sensor);
            number--;
        }
        return sensors;
    }

    /**
     * Test 11
     * If the system is armed-home while the camera shows a cat, set the alarm status to alarm
     */
    @Test
    @DisplayName("If the system is armed-home while the camera shows a cat, set the alarm status to alarm")
    public void changeStatusToAlarmIfSystemIsArmedHomeAndCameraShowsACat() {
        doReturn(ArmingStatus.ARMED_HOME).when(repository).getArmingStatus();
        doReturn(true).when(imageServiceInstance).imageContainsCat(any(BufferedImage.class), anyFloat());
        service.processImage(bufferedImage);
        verify(repository, times(1)).setAlarmStatus(alarmArgumentCaptor.capture());
        AlarmStatus actual = alarmArgumentCaptor.getValue();
        AlarmStatus expected = AlarmStatus.ALARM;
        assertEquals(expected, actual);
    }
}