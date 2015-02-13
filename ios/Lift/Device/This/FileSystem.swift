import Foundation

///
/// Handles session data storage in file
///
struct File {
    
    static var currentSessionId = 0
    static var currentFile: File? = nil
    
    ///
    /// Creates a new file to store session data
    ///
    /* mutating */
    static func newSession() {
        currentFile = File(++currentSessionId)
    }
    
    ///
    /// Appends ``data`` to the current file
    ///
    static func writeToFile(data: NSData) {
        currentFile!.writeData(data)
    }
    
    ///
    /// Closes the current session file
    ///
    /* mutating */
    static func closeCurrent() {
        currentFile?.close()
    }
    
    private let fileHandle: NSFileHandle!
    
    ///
    /// Initializes a new session file for ``id``
    init (_ id: Int) {
        let directoryUrl = NSFileManager.defaultManager().URLsForDirectory(NSSearchPathDirectory.DocumentDirectory, inDomains: NSSearchPathDomainMask.UserDomainMask)[0] as NSURL
        let url = directoryUrl.URLByAppendingPathComponent("session_\(id).dat")
        NSFileManager.defaultManager().createFileAtPath(url.path!, contents: NSData(), attributes: nil)
        var error = NSErrorPointer()
        fileHandle = NSFileHandle(forWritingToURL: url, error: error)
    }
    
    ///
    /// Appends ``data`` to the file
    ///
    /* side-effecting */
    func writeData(data: NSData) {
        fileHandle.writeData(data)
    }
    
    ///
    /// Closes the file
    ///
    func close() {
        fileHandle.closeFile()
    }
    
}
