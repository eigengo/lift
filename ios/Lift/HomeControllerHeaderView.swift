import Foundation

protocol HomeControllerHeaderViewDelegate {
    
    func headerDateSelected(date: NSDate)
    
    func headerSessionsOnDate(date: NSDate) -> Exercise.SessionDate?
    
}

class HomeControllerHeaderView : UIView, JTCalendarDataSource {
    @IBOutlet var profileImageView: UIImageView!
    @IBOutlet var bottomView: UIView!
    @IBOutlet var nameLabel: UILabel!
    @IBOutlet var calendarContentView: JTCalendarContentView!
    private var delegate: HomeControllerHeaderViewDelegate?
    private let calendar = JTCalendar()
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        profileImageView.layer.borderColor = tintColor.CGColor
        profileImageView.layer.borderWidth = 3
        profileImageView.layer.cornerRadius = profileImageView.frame.width / 2
        profileImageView.clipsToBounds = true
        
        backgroundColor = UIColor.clearColor()
        
        calendar.calendarAppearance.isWeekMode = true
        calendar.menuMonthsView = JTCalendarMenuView()
        calendar.contentView = calendarContentView

        // TODO: in a setter of some kind
        calendar.dataSource = self
        calendar.currentDate = NSDate()
        calendar.currentDateSelected = NSDate()
    }
    
    func setDelegate(delegate: HomeControllerHeaderViewDelegate) {
        self.delegate = delegate
        reloadData()
    }
    
    func setPublicProfile(profile: User.PublicProfile?) {
        if let x = profile {
            nameLabel.text = x.firstName + " " + x.lastName
        } else {
            nameLabel.text = "User"
        }
    }
    
    func setProfileImage(image: UIImage) {
        profileImageView.image = image
    }
    
    override func drawRect(rect: CGRect) {
        var r: CGFloat = 0
        var g: CGFloat = 0
        var b: CGFloat = 0
        var a: CGFloat = 0
        tintColor.getRed(&r, green: &g, blue: &b, alpha: &a)

        let ctx = UIGraphicsGetCurrentContext()
        CGContextSetLineWidth(ctx, 4)
        CGContextSetRGBStrokeColor(ctx, r, g, b, a)
        CGContextBeginPath(ctx)
        CGContextMoveToPoint(ctx, CGRectGetMinX(rect), bottomView.frame.origin.y)
        CGContextAddLineToPoint(ctx, CGRectGetMaxX(rect), bottomView.frame.origin.y)
        CGContextStrokePath(ctx)
        
        super.drawRect(rect)
    }
    
    func reloadData() {
        calendar.reloadData()
        calendar.currentDate = NSDate()
        calendar.currentDateSelected = NSDate()
        delegate?.headerDateSelected(NSDate())
    }
    
    // MARK: JTCalendarDataSource
    
    func calendarHaveEvent(calendar: JTCalendar!, date: NSDate!) -> Bool {
        if let x = delegate {
            return x.headerSessionsOnDate(date) != nil
        }
        
        return false
    }
    
    func calendarDidDateSelected(calendar: JTCalendar!, date: NSDate!) {
        delegate?.headerDateSelected(date)
    }

}