import Foundation
import UIKit

@objc
protocol MultiDeviceSessionSettable {
    
    func setMultiDeviceSession(multi: MultiDeviceSession)
    
}

class LiveSessionController: UIPageViewController, ExerciseSessionSettable, DeviceSessionDelegate, DeviceDelegate {
    private var multi: MultiDeviceSession?
    private var timer: NSTimer?
    private var startTime: NSDate?
    private var exerciseSession: ExerciseSession?
    @IBOutlet var stopSessionButton: UIBarButtonItem!
    
    // MARK: main
    override func viewWillDisappear(animated: Bool) {
        if let x = timer { x.invalidate() }
        navigationItem.prompt = nil
    }
    
    @IBAction
    func stopSession() {
        if stopSessionButton.tag < 0 {
            stopSessionButton.title = "Really?".localized()
            stopSessionButton.tag = 3
        } else {
            end()
        }
    }
    
    func end() {
        if let x = exerciseSession {
            x.end(const(()))
            self.exerciseSession = nil
        } else {
            NSLog("[WARN] LiveSessionController.end() with sessionId == nil")
        }
        
        multi?.stop()
        UIApplication.sharedApplication().idleTimerDisabled = false
        if let x = navigationController {
            x.popToRootViewControllerAnimated(true)
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        startTime = NSDate()
        timer = NSTimer.scheduledTimerWithTimeInterval(1, target: self, selector: "tick", userInfo: nil, repeats: true)
    }
    
    func tick() {
        let elapsed = Int(NSDate().timeIntervalSinceDate(startTime!))
        let minutes: Int = elapsed / 60
        let seconds: Int = elapsed - minutes * 60
        navigationItem.prompt = "LiveSessionController.elapsed".localized(minutes, seconds)
        stopSessionButton.tag -= 1
        if stopSessionButton.tag < 0 {
            stopSessionButton.title = "Stop".localized()
        }
    }
    
    // MARK: ExerciseSessionSettable
    func setExerciseSession(session: ExerciseSession) {
        self.exerciseSession = session
        multi = MultiDeviceSession(deviceDelegate: self, deviceSessionDelegate: self)
        multi!.start()
        UIApplication.sharedApplication().idleTimerDisabled = true
    }
    
    private func updateMulti(ctrl: AnyObject) {
        if let x = ctrl as? MultiDeviceSessionSettable {
            if let m = multi { x.setMultiDeviceSession(m) }
        }
    }
    
    // MARK: DeviceSessionDelegate
    func deviceSession(session: DeviceSession, finishedWarmingUp deviceId: DeviceId) {
        // ???
    }
    
    func deviceSession(session: DeviceSession, startedWarmingUp deviceId: DeviceId, expectedCompletionIn time: NSTimeInterval) {
        // ???
    }
    
    func deviceSession(session: DeviceSession, endedFrom deviceId: DeviceId) {
        end()
    }
    
    func deviceSession(session: DeviceSession, sensorDataNotReceivedFrom deviceId: DeviceId) {
        // ???
    }
    
    func deviceSession(session: DeviceSession, sensorDataReceivedFrom deviceId: DeviceId, atDeviceTime: CFAbsoluteTime, data: NSData) {
        if let x = exerciseSession {
            x.submitData(data, f: const(()))
            
            if UIApplication.sharedApplication().applicationState != UIApplicationState.Background {
                viewControllers.foreach(updateMulti)
            }
        } else {
            RKDropdownAlert.title("Internal inconsistency", message: "AD received, but no sessionId.", backgroundColor: UIColor.orangeColor(), textColor: UIColor.blackColor(), time: 3)
        }
    }
    
    // MARK: DeviceDelegate
    func deviceGotDeviceInfo(deviceId: DeviceId, deviceInfo: DeviceInfo) {
        viewControllers.foreach(updateMulti)
    }
    
    func deviceGotDeviceInfoDetail(deviceId: DeviceId, detail: DeviceInfo.Detail) {
        viewControllers.foreach(updateMulti)
    }
    
    func deviceAppLaunched(deviceId: DeviceId) {
        //
    }
    
    func deviceAppLaunchFailed(deviceId: DeviceId, error: NSError) {
        //
    }
    
    func deviceDidNotConnect(error: NSError) {
        viewControllers.foreach(updateMulti)
    }
    
    func deviceDisconnected(deviceId: DeviceId) {
        viewControllers.foreach(updateMulti)
    }
    
}