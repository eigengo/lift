import Foundation

///
/// Implementatiosn will receive the given ``alert``
///
@objc
protocol RemoteNotificationDelegate {
    
    func remoteNotificationReceived(#alert: String)
    
    func remoteNotificatioReceived(#data: NSData)
    
}
