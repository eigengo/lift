import Foundation

@objc
protocol RemoteNotificationDelegate {
    
    func remoteNotificationReceivedAlert(alert: String)
    
}
