# Ex7 — ISP: Smart Classroom Devices Interface

## 1. Context
A smart classroom controller manages devices: projector, lights, AC, attendance scanner.

## 2. Current behavior
- There is one large interface `SmartClassroomDevice` containing many methods
- Each device implements methods it does not need using dummy logic
- Controller calls only some methods depending on device type

## 3. What’s wrong (at least 5 issues)
1. Fat interface forces irrelevant methods on devices.
2. Dummy implementations hide bugs and create misleading behavior.
3. Controller is tempted to call methods that some devices don’t meaningfully support.
4. Adding a new device forces implementing many unrelated methods.
5. Device capabilities are unclear; interface does not model reality.

## 4. Your task
- Split the fat interface into smaller capability-based interfaces.
- Update controller and devices to depend only on what they use.
- Preserve console output.

## 5. Constraints
- Preserve output for `Main`.
- Keep device class names unchanged.
- No external libs.

## 6. Acceptance criteria
- No device implements methods irrelevant to it.
- Controller depends only on specific capability interfaces.

## 7. How to run
```bash
cd SOLID/Ex7/src
javac *.java
java Main
```

## 8. Sample output
```text
=== Smart Classroom ===
Projector ON (HDMI-1)
Lights set to 60%
AC set to 24C
Attendance scanned: present=3
Shutdown sequence:
Projector OFF
Lights OFF
AC OFF
```

## 9. Hints (OOP-only)
- Capabilities: power control, brightness control, temperature control, scanning.
- Keep composition: registry can return devices by capability rather than by concrete class.

## 10. Stretch goals
- Add a “smart board” device without implementing unrelated methods.

---

# Preparation Notes (Diagram Style)

## 11. Current Design

```
┌──────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│──────────────────────────────────────────────────────────────│
│  DeviceRegistry reg = new DeviceRegistry();                  │
│  reg.add(new Projector());                                   │
│  reg.add(new LightsPanel());                                 │
│  reg.add(new AirConditioner());                              │
│  reg.add(new AttendanceScanner());                           │
│                                                              │
│  ClassroomController ctrl = new ClassroomController(reg);   │
│  ctrl.run();                                                 │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                 ClassroomController.run()                    │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  SmartClassroomDevice proj    = reg.get(0); ← FAT TYPE       │
│  SmartClassroomDevice lights  = reg.get(1); ← FAT TYPE       │
│  SmartClassroomDevice ac      = reg.get(2); ← FAT TYPE       │
│  SmartClassroomDevice scanner = reg.get(3); ← FAT TYPE       │
│                                                              │
│  proj.powerOn();                                             │
│  proj.connectInput("HDMI-1");  → "Projector ON (HDMI-1)"    │
│                                                              │
│  lights.powerOn();                                           │
│  lights.setBrightness(60);     → "Lights set to 60%"        │
│                                                              │
│  ac.powerOn();                                               │
│  ac.setTemperatureC(24);       → "AC set to 24C"            │
│                                                              │
│  int n = scanner.scanAttendance();                           │
│           → "Attendance scanned: present=3"                  │
│                                                              │
│  proj.powerOff();  lights.powerOff();  ac.powerOff();        │
│                                                              │
│  ⚠ All variables typed as SmartClassroomDevice — the fat    │
│    interface. Controller sees all 6 methods on every device. │
│    scanner.setBrightness(60) would COMPILE with no warning.  │
└──────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│         «interface» SmartClassroomDevice                     │
│──────────────────────────────────────────────────────────────│
│  void powerOn()                                              │
│  void powerOff()                                             │
│  void setBrightness(int pct)                                 │
│  void setTemperatureC(int c)                                 │
│  int  scanAttendance()                                       │
│  void connectInput(String port)                              │
│                                                              │
│  ⚠ 6 METHODS — only subsets are relevant to each device.    │
└──────┬────────────┬────────────┬────────────┬────────────────┘
       │            │            │            │
       ▼            ▼            ▼            ▼
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────────────┐
│ Projector  │ │ LightsPanel│ │AirCondit-  │ │AttendanceScanner │
│────────────│ │────────────│ │ioner       │ │──────────────────│
│ powerOn  ✓ │ │ powerOn  ✓ │ │────────────│ │ scanAttend.  ✓   │
│ powerOff ✓ │ │ powerOff ✓ │ │ powerOn  ✓ │ │                  │
│ connect-   │ │ setBright  │ │ powerOff ✓ │ │ powerOn  /* ok */│
│  Input   ✓ │ │  ness    ✓ │ │ setTempC ✓ │ │ powerOff /* n/a*/│
│            │ │            │ │            │ │ setBright/* n/a*/│
│ setBright  │ │ setTempC   │ │ setBright  │ │ setTempC /* n/a*/│
│ /* irrel */│ │ /* irrel */│ │ /* irrel */│ │ connectIn/* n/a*/│
│ setTempC   │ │ scanAtten  │ │ scanAtten  │ │                  │
│ /* irrel */│ │ /* irrel */│ │ /* irrel */│ │ ⚠ 5 of 6 methods│
│ scanAtten  │ │ connectIn  │ │ connectIn  │ │   are dummies    │
│  return 0  │ │ /* irrel */│ │ /* irrel */│ │                  │
│            │ │            │ │            │ │                  │
│ ⚠ 3 dummy │ │ ⚠ 3 dummy │ │ ⚠ 3 dummy │ │                  │
└────────────┘ └────────────┘ └────────────┘ └──────────────────┘

┌──────────────────────────────┐
│       DeviceRegistry         │
│──────────────────────────────│
│  List<SmartClassroomDevice>  │
│  add(SmartClassroomDevice d) │
│  get(int i) →                │
│    SmartClassroomDevice      │
│                              │
│  ⚠ Typed to fat interface.  │
│    Every device appears to   │
│    support all 6 methods.    │
└──────────────────────────────┘
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: Projector forced to implement 3 dummy methods      │
│                                          [ISP VIOLATION]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: Projector.java                                       │
│                                                              │
│  Projector is a power + input-connection device.             │
│  It has NO brightness, NO temperature, NO attendance.        │
│                                                              │
│  FORCED DUMMIES:                                             │
│    setBrightness(int pct)  { /* irrelevant */ }              │
│    setTemperatureC(int c)  { /* irrelevant */ }              │
│    scanAttendance()        { return 0; }                     │
│                                                              │
│  ISP RULE: No class should be forced to implement methods    │
│  it does not use.                                            │
│                                                              │
│  RISK: A future refactor accidentally executes a dummy       │
│  method and the system silently does nothing or returns 0.   │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: LightsPanel forced to implement 3 dummy methods    │
│                                          [ISP VIOLATION]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: LightsPanel.java                                     │
│                                                              │
│  LightsPanel needs: powerOn, powerOff, setBrightness.        │
│  It has NO temperature control, NO scanner, NO input port.   │
│                                                              │
│  FORCED DUMMIES:                                             │
│    setTemperatureC(int c)  { /* irrelevant */ }              │
│    scanAttendance()        { return 0; }                     │
│    connectInput(String p)  { /* irrelevant */ }              │
│                                                              │
│  Adding any new method to SmartClassroomDevice forces        │
│  LightsPanel to add it too — regardless of relevance.        │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: AirConditioner forced to implement 3 dummy methods │
│                                          [ISP VIOLATION]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: AirConditioner.java                                  │
│                                                              │
│  AirConditioner needs: powerOn, powerOff, setTemperatureC.   │
│  It has NO brightness, NO scanner, NO input port.            │
│                                                              │
│  FORCED DUMMIES:                                             │
│    setBrightness(int pct)  { /* irrelevant */ }              │
│    scanAttendance()        { return 0; }                     │
│    connectInput(String p)  { /* irrelevant */ }              │
│                                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: AttendanceScanner — 5 of 6 methods are dummies     │
│                                          [ISP VIOLATION]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: AttendanceScanner.java                               │
│                                                              │
│  AttendanceScanner needs ONLY: scanAttendance.               │
│  It has no power cycle, no brightness, no temp, no port.     │
│                                                              │
│  FORCED DUMMIES:                                             │
│    powerOn()               { /* ok but no output */ }        │
│    powerOff()              { /* no output */ }               │
│    setBrightness(int pct)  { /* irrelevant */ }              │
│    setTemperatureC(int c)  { /* irrelevant */ }              │
│    connectInput(String p)  { /* irrelevant */ }              │
│                                                              │
│  The WORST offender: 5 out of 6 methods are meaningless.     │
│  The fat interface has overwhelmed this device's real concern.│
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 5: DeviceRegistry typed to fat interface              │
│           — controller can call wrong methods on any device  │
│                                          [ISP VIOLATION]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: DeviceRegistry.java + ClassroomController.java       │
│                                                              │
│  List<SmartClassroomDevice> devices;                         │
│  SmartClassroomDevice get(int i)                             │
│                                                              │
│  Because every variable is typed as SmartClassroomDevice,    │
│  the controller CAN call:                                    │
│    scanner.setBrightness(60)   ← compiles, no error          │
│    proj.setTemperatureC(24)    ← compiles, no error          │
│    ac.scanAttendance()         ← compiles, returns 0         │
│                                                              │
│  None of these are caught by the compiler.                   │
│  The fat interface provides ZERO guidance about which        │
│  capabilities belong to which device.                        │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│  Split SmartClassroomDevice → 5 narrow capability interfaces │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE (one fat interface):                                 │
│    interface SmartClassroomDevice {                          │
│        powerOn(); powerOff(); setBrightness(int);            │
│        setTemperatureC(int); scanAttendance();               │
│        connectInput(String);                                 │
│    }                                                         │
│                                                              │
│  AFTER (5 focused capability interfaces):                    │
│                                                              │
│  ┌────────────────────┐  ┌──────────────────────────┐        │
│  │ PowerControllable  │  │  BrightnessControllable  │        │
│  │────────────────────│  │──────────────────────────│        │
│  │ void powerOn()     │  │  void setBrightness(int) │        │
│  │ void powerOff()    │  └──────────────────────────┘        │
│  └────────────────────┘                                      │
│  ┌──────────────────────────┐  ┌──────────────────┐          │
│  │ TemperatureControllable  │  │ Scannable        │          │
│  │──────────────────────────│  │──────────────────│          │
│  │ void setTemperatureC(int)│  │ int scanAtten.() │          │
│  └──────────────────────────┘  └──────────────────┘          │
│  ┌──────────────────────────┐                                │
│  │ InputConnectable         │                                │
│  │──────────────────────────│                                │
│  │ void connectInput(String)│                                │
│  └──────────────────────────┘                                │
│                                                              │
│  Each interface is narrow — exactly one capability.          │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  Each device implements ONLY what it needs                   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  Projector implements PowerControllable, InputConnectable    │
│    powerOn, powerOff, connectInput  ← REAL implementations   │
│    NO setBrightness. NO setTemperatureC. NO scanAttendance.  │
│    NO dummy methods.                                         │
│                                                              │
│  LightsPanel implements PowerControllable,                   │
│                           BrightnessControllable             │
│    powerOn, powerOff, setBrightness  ← REAL implementations  │
│    NO dummy methods.                                         │
│                                                              │
│  AirConditioner implements PowerControllable,                │
│                              TemperatureControllable         │
│    powerOn, powerOff, setTemperatureC  ← REAL implementations│
│    NO dummy methods.                                         │
│                                                              │
│  AttendanceScanner implements Scannable                      │
│    scanAttendance()  ← the ONE method it needs               │
│    NO powerOn. NO powerOff. NO dummy methods.                │
│                                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ClassroomController — casts to specific capability types    │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    SmartClassroomDevice proj = reg.get(0);                   │
│    proj.setBrightness(60);  ← compiles, WRONG device         │
│    proj.scanAttendance();   ← compiles, returns dummy 0      │
│                                                              │
│  AFTER:                                                      │
│    PowerControllable proj =                                  │
│        (PowerControllable) reg.get(0);                       │
│    InputConnectable projInput =                              │
│        (InputConnectable) reg.get(0);                        │
│    projInput.connectInput("HDMI-1");                         │
│                                                              │
│    BrightnessControllable lights =                           │
│        (BrightnessControllable) reg.get(1);                  │
│    lights.setBrightness(60);                                 │
│                                                              │
│    TemperatureControllable ac =                              │
│        (TemperatureControllable) reg.get(2);                 │
│    ac.setTemperatureC(24);                                   │
│                                                              │
│    Scannable scanner = (Scannable) reg.get(3);               │
│    int n = scanner.scanAttendance();                         │
│                                                              │
│  Compiler now enforces correct capability per device.        │
│  lights.setTemperatureC(24) → COMPILE ERROR.                 │
│  Wrong capability on wrong device = caught at compile time.  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  DeviceRegistry — stores Object instead of fat interface     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    List<SmartClassroomDevice>                                │
│    add(SmartClassroomDevice d)                               │
│    SmartClassroomDevice get(int i)                           │
│                                                              │
│  AFTER:                                                      │
│    List<Object>                                              │
│    add(Object d)                                             │
│    Object get(int i)                                         │
│                                                              │
│  Registry no longer forces a fat interface on every device.  │
│  Devices are stored as their real type.                      │
│  The controller casts out to the capability it needs.        │
│  Attempting a wrong cast → ClassCastException at runtime,    │
│  a clear signal of a design mistake rather than silent dummy.│
└──────────────────────────────────────────────────────────────┘


WHAT CHANGED — BEFORE vs AFTER:

  BEFORE                               AFTER
  ──────                               ─────
  1 fat interface (6 methods)    →    5 narrow capability interfaces
  Projector: 3 dummy methods     →    0 dummy methods
  LightsPanel: 3 dummy methods   →    0 dummy methods
  AirConditioner: 3 dummy methods→    0 dummy methods
  AttendanceScanner: 5 dummies   →    0 dummy methods
  DeviceRegistry: fat type       →    List<Object>
  Controller: SmartClassroomDev  →    specific capability types


ISP PROOF:

  New "SmartBoard" (power + input + brightness)?
    → SmartBoard implements PowerControllable,
                              InputConnectable,
                              BrightnessControllable
    → NO setTemperatureC. NO scanAttendance.
    → NO dummy methods. ISP satisfied.

  Every class depends ONLY on the methods it actually uses.
```

