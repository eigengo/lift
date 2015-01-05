import Foundation

protocol HomeControllerHeaderViewDelegate {
    
    func editProfile()
    
    func settings()
    
}

class RandomCalendarDataSource : NSObject, JTCalendarDataSource {
    func calendarHaveEvent(calendar: JTCalendar!, date: NSDate!) -> Bool {
        if NSDate().compare(date) == NSComparisonResult.OrderedAscending { return false } else { return random() % 3 == 0 }
    }
    
    func calendarDidDateSelected(calendar: JTCalendar!, date: NSDate!) {
        
    }
}

class HomeControllerHeaderView : UIView {
    @IBOutlet var profileImageView: UIImageView!
    @IBOutlet var bottomView: UIView!
    @IBOutlet var editProfileButton: UIButton!
    @IBOutlet var settingsButton: UIButton!
    @IBOutlet var nameLabel: UILabel!
    @IBOutlet var calendarContentView: JTCalendarContentView!
    // TODO: allow to be set
    private var calendarDataSource: JTCalendarDataSource? = RandomCalendarDataSource()
    private var delegate: HomeControllerHeaderViewDelegate?
    private let calendar = JTCalendar()
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        profileImageView.layer.borderColor = tintColor.CGColor
        profileImageView.layer.borderWidth = 3
        profileImageView.layer.cornerRadius = profileImageView.frame.width / 2
        profileImageView.clipsToBounds = true
        
        editProfileButton.roundedBorder(tintColor)
        settingsButton.roundedBorder(tintColor)
        
        backgroundColor = UIColor.clearColor()
        
        calendar.calendarAppearance.isWeekMode = true
        calendar.menuMonthsView = JTCalendarMenuView()
        calendar.contentView = calendarContentView

        // TODO: in a setter of some kind
        calendar.dataSource = calendarDataSource
        calendar.currentDate = NSDate()
        calendar.currentDateSelected = NSDate()
        calendar.reloadData()
    }
    
    func setDelegate(delegate: HomeControllerHeaderViewDelegate) {
        self.delegate = delegate
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
    
    // MARK: Actions
    
    @IBAction func editProfile() {
        delegate?.editProfile()
    }
    
    @IBAction func settings() {
        delegate?.settings()
    }
}