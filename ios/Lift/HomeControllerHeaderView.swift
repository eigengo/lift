import Foundation

class HomeControllerHeaderView : UIView {
    @IBOutlet var profileImageView: UIImageView!
    @IBOutlet var bottomView: UIView!
    @IBOutlet var editProfileButton: UIButton!
    @IBOutlet var settingsButton: UIButton!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        profileImageView.layer.borderColor = tintColor.CGColor
        profileImageView.layer.borderWidth = 3
        profileImageView.layer.cornerRadius = profileImageView.frame.width / 2
        profileImageView.clipsToBounds = true
        
        editProfileButton.roundedBorder(tintColor)
        settingsButton.roundedBorder(tintColor)
        
        backgroundColor = UIColor.clearColor()
        alpha = 1
    }
    
    func setPublicProfile(profile: User.PublicProfile?) {
        
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
}