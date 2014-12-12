import UIKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var deviceToken: NSData?
    var window: UIWindow?
    var alertView: UIAlertView? = nil

    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
        if UIDevice.currentDevice().systemVersion >= "8.0" {
            let settings = UIUserNotificationSettings(forTypes: UIUserNotificationType.Alert | UIUserNotificationType.Badge | UIUserNotificationType.Sound, categories: nil)
            UIApplication.sharedApplication().registerUserNotificationSettings(settings)
        } else {
            UIApplication.sharedApplication().registerForRemoteNotificationTypes(UIRemoteNotificationType.Alert | UIRemoteNotificationType.Badge | UIRemoteNotificationType.Sound)
        }

        let type = UIUserNotificationType.Alert | UIUserNotificationType.Badge | UIUserNotificationType.Sound
        let settings = UIUserNotificationSettings(forTypes: type, categories: nil)
        UIApplication.sharedApplication().registerUserNotificationSettings(settings)
        
        return true
    }
        
    func application(application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: NSData) {
        self.deviceToken = deviceToken
        NSLog("Token \(deviceToken)")
    }
    
    func application(application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: NSError) {
        let data: [UInt8] = [0x5A, 0xB8, 0x48, 0x05, 0xF8, 0xD0, 0xCC, 0x63, 0x0A, 0x89, 0x90, 0xA8, 0x4D, 0x48, 0x08, 0x41, 0xC3, 0x68, 0x40, 0x03, 0x6C, 0x12, 0x2C, 0x8E, 0x52, 0xA8, 0xDC, 0xFD, 0x68, 0xA6, 0xF6, 0xF8]
        let buf = UnsafePointer<[UInt8]>(data)
        let deviceToken = NSData(bytes: buf, length: data.count)
        self.deviceToken = deviceToken
        NSLog("Not registered \(error)")
    }
    
    func application(application: UIApplication, didRegisterUserNotificationSettings notificationSettings: UIUserNotificationSettings) {
        application.registerForRemoteNotifications()
    }
    
    func application(application: UIApplication, didReceiveRemoteNotification userInfo: [NSObject : AnyObject]) {
        if self.alertView == nil {
            let aps = userInfo["aps"] as [NSObject : AnyObject]
            let alert = aps["alert"] as String
            
            AudioServicesPlayAlertSound(1007)
            self.alertView = UIAlertView(title: "Exercise", message: alert, delegate: nil, cancelButtonTitle: nil)
            self.alertView!.show()
            let delay = dispatch_time(DISPATCH_TIME_NOW, Int64(2 * Double(NSEC_PER_SEC)))
            dispatch_after(delay, dispatch_get_main_queue()) {
                self.alertView!.dismissWithClickedButtonIndex(0, animated: true)
                self.alertView = nil
            }
        }
    }
    
    func applicationDidBecomeActive(application: UIApplication) {
        LiftServerCache.sharedInstance.build()
    }
    
    func applicationDidReceiveMemoryWarning(application: UIApplication) {
        LiftServerCache.sharedInstance.clean()
    }
}

