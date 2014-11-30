import UIKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, PBPebbleCentralDelegate, PBWatchDelegate {

    var window: UIWindow?

    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
        if UIDevice.currentDevice().systemVersion >= "8.0" {
            let settings = UIUserNotificationSettings(forTypes: UIUserNotificationType.Alert | UIUserNotificationType.Badge | UIUserNotificationType.Sound, categories: nil)
            UIApplication.sharedApplication().registerUserNotificationSettings(settings)
        } else {
            UIApplication.sharedApplication().registerForRemoteNotificationTypes(UIRemoteNotificationType.Alert | UIRemoteNotificationType.Badge | UIRemoteNotificationType.Sound)
        }
        
        let uuid = NSMutableData(length: 16)!
        NSUUID(UUIDString: "E113DED8-0EA6-4397-90FA-CE40941F7CBC")!.getUUIDBytes(UnsafeMutablePointer(uuid.mutableBytes))
        PBPebbleCentral.setDebugLogsEnabled(true)
        let central = PBPebbleCentral.defaultCentral()
        central.appUUID = uuid
        central.delegate = self
        for w in central.connectedWatches {
            launchLiftPebbleApp(w as PBWatch)
        }
        
        return true
    }
    
    func launchLiftPebbleApp(watch: PBWatch!) {
        watch.appMessagesLaunch({ (watch: PBWatch!, error: NSError!) -> Void in
            if (error != nil) {
                NSLog(":(")
            } else {
                NSLog(":)")
            }
        }, withUUID: PBPebbleCentral.defaultCentral()!.appUUID)
    }
    
    func pebbleCentral(central: PBPebbleCentral!, watchDidConnect watch: PBWatch!, isNew: Bool) {
        NSLog("Connected %@", watch)
        PBPebbleCentral.defaultCentral().dataLoggingService.pollForData()
    }
    
    func pebbleCentral(central: PBPebbleCentral!, watchDidDisconnect watch: PBWatch!) {
        PBPebbleCentral.defaultCentral().dataLoggingService.pollForData()
        NSLog("Gone %@", watch)
    }
    
    func application(application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: NSData) {
        NSLog("Token \(deviceToken)")
    }
    
    func application(application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: NSError) {
        NSLog("Not registered \(error)")
    }
    
    func application(application: UIApplication, didRegisterUserNotificationSettings notificationSettings: UIUserNotificationSettings) {
        application.registerForRemoteNotifications()
    }
    
}

