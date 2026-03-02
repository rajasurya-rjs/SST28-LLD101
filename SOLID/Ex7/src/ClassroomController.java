public class ClassroomController {
    private final DeviceRegistry reg;

    public ClassroomController(DeviceRegistry reg) { this.reg = reg; }

    public void startClass() {
        PowerControllable pj = (PowerControllable) reg.getFirstOfType("Projector");
        pj.powerOn();
        ((InputConnectable) pj).connectInput("HDMI-1");

        BrightnessControllable lights = (BrightnessControllable) reg.getFirstOfType("LightsPanel");
        lights.setBrightness(60);

        TemperatureControllable ac = (TemperatureControllable) reg.getFirstOfType("AirConditioner");
        ac.setTemperatureC(24);

        Scannable scan = (Scannable) reg.getFirstOfType("AttendanceScanner");
        System.out.println("Attendance scanned: present=" + scan.scanAttendance());
    }

    public void endClass() {
        System.out.println("Shutdown sequence:");
        ((PowerControllable) reg.getFirstOfType("Projector")).powerOff();
        ((PowerControllable) reg.getFirstOfType("LightsPanel")).powerOff();
        ((PowerControllable) reg.getFirstOfType("AirConditioner")).powerOff();
    }
}
