import Foundation
import UIKit

@objc
protocol MultiDeviceSessionSettable {
    
    func setMultiDeviceSession(multi: MultiDeviceSession)
    
}

class LiveSessionController: UIPageViewController, UIPageViewControllerDataSource, ExerciseSessionSettable, DeviceSessionDelegate, DeviceDelegate {
    private var multi: MultiDeviceSession?
    private var timer: NSTimer?
    private var startTime: NSDate?
    private var exerciseSession: ExerciseSession?
    private var pageViewControllers: [UIViewController] = []
    private var pageControl: UIPageControl!
    @IBOutlet var stopSessionButton: UIBarButtonItem!
    
    // MARK: main
    override func viewWillDisappear(animated: Bool) {
        if let x = timer { x.invalidate() }
        navigationItem.prompt = nil
    }
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
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
        dataSource = self

        let pagesStoryboard = UIStoryboard(name: "LiveSession", bundle: nil)
        pageViewControllers = ["devices", "sensorDataGroup", "classification"].map { pagesStoryboard.instantiateViewControllerWithIdentifier($0) as UIViewController }
        setViewControllers([pageViewControllers.first!], direction: UIPageViewControllerNavigationDirection.Forward, animated: false, completion: nil)
        
        if let nc = navigationController {
            let navBarSize = nc.navigationBar.bounds.size
            let origin = CGPoint(x: navBarSize.width / 2, y: navBarSize.height / 2 + navBarSize.height / 4)
            pageControl = UIPageControl(frame: CGRect(x: origin.x, y: origin.y, width: 0, height: 0))
            pageControl.numberOfPages = 3
            nc.navigationBar.addSubview(pageControl)
        }
        
        viewControllers.foreach(updateMulti)
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
    
    // MARK: UIPageViewControllerDataSource
    func pageViewController(pageViewController: UIPageViewController, viewControllerAfterViewController viewController: UIViewController) -> UIViewController? {
        if let x = (pageViewControllers.indexOf { $0 === viewController }) {
            if x < pageViewControllers.count - 1 { return pageViewControllers[x + 1] }
        }
        return nil
    }
    
    func pageViewController(pageViewController: UIPageViewController, viewControllerBeforeViewController viewController: UIViewController) -> UIViewController? {
        if let x = (pageViewControllers.indexOf { $0 === viewController }) {
            if x > 0 { return pageViewControllers[x - 1] }
        }
        return nil
    }
    
    // MARK: DeviceSessionDelegate
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
