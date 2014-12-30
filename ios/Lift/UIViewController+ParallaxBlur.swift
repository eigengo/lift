import Foundation

internal class UIParallaxViewController : UIViewController, UIScrollViewDelegate {
    private var headerOverlayView: UIView?
    // TODO: height here
    private let imageHeight: CGFloat = 320.0
    private let headerHeight: CGFloat = 60.0
    private let invisDelta: CGFloat = 50.0
    private let blurDistance: CGFloat = 200.0

    private let mainScrollView: UIScrollView = {
        let scrollView = UIScrollView()
        scrollView.bounces = true
        scrollView.alwaysBounceVertical = true
        scrollView.showsVerticalScrollIndicator = true
        scrollView.autoresizingMask = UIViewAutoresizing.FlexibleHeight | UIViewAutoresizing.FlexibleWidth
        scrollView.autoresizesSubviews = true
        return scrollView
    }()
    private let backgroundScrollView: UIScrollView = {
       let backgroundView = UIScrollView()
        backgroundView.scrollEnabled = false
        backgroundView.autoresizingMask = UIViewAutoresizing.FlexibleWidth
        backgroundView.autoresizesSubviews = true
        return backgroundView
    }()
    private let headerImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = UIViewContentMode.ScaleAspectFill
        imageView.autoresizingMask = UIViewAutoresizing.FlexibleWidth | UIViewAutoresizing.FlexibleHeight
        return imageView
    }()
    private let blurredImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = UIViewContentMode.ScaleAspectFill
        imageView.autoresizingMask = UIViewAutoresizing.FlexibleWidth | UIViewAutoresizing.FlexibleHeight
        imageView.alpha = 0
        return imageView
    }()
    private let floatingHeaderView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.clearColor()
        let be = UIBlurEffect(style: UIBlurEffectStyle.Light)
        let ve = UIVibrancyEffect(forBlurEffect: be)
        let vew = UIVisualEffectView(effect: ve)
        return vew
    }()
    private let scrollViewContainer: UIView = {
        let svc = UIView()
        svc.autoresizingMask = UIViewAutoresizing.FlexibleWidth
        return svc
    }()
    private var cv: UIScrollView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // set up the holding view
        cv = contentView()
        cv.autoresizingMask = UIViewAutoresizing.FlexibleWidth
        
        // set our view to be the scroll view
        mainScrollView.contentSize = CGSizeMake(view.frame.size.width, 1000)
        mainScrollView.delegate = self
        view = mainScrollView
        
        // background scroll view
        backgroundScrollView.contentSize = CGSizeMake(view.frame.size.width, 1000)

        // set up frames
        mainScrollView.frame = view.frame
        backgroundScrollView.frame = CGRectMake(0, 0, CGRectGetWidth(view.frame), imageHeight)
        headerImageView.frame = CGRectMake(0, 0, CGRectGetWidth(backgroundScrollView.frame), CGRectGetHeight(backgroundScrollView.frame))
        blurredImageView.frame = CGRectMake(0, 0, CGRectGetWidth(backgroundScrollView.frame), CGRectGetHeight(backgroundScrollView.frame))
        floatingHeaderView.frame = backgroundScrollView.frame
        scrollViewContainer.frame = CGRectMake(0, CGRectGetHeight(backgroundScrollView.frame), CGRectGetWidth(view.frame), CGRectGetHeight(view.frame) - offsetHeight())
        
        // set up the view structure
        backgroundScrollView.addSubview(headerImageView)
        backgroundScrollView.addSubview(blurredImageView)
        scrollViewContainer.addSubview(cv!)
        mainScrollView.addSubview(backgroundScrollView)
        mainScrollView.addSubview(floatingHeaderView)
        mainScrollView.addSubview(scrollViewContainer)
    }

    override func viewWillAppear(animated: Bool) {
        cv.frame = CGRectMake(0, 0, CGRectGetWidth(scrollViewContainer.frame), CGRectGetHeight(view.frame) - offsetHeight())
    }
    
    override func viewDidAppear(animated: Bool) {
        mainScrollView.contentSize = CGSizeMake(CGRectGetWidth(view.frame), cv.contentSize.height + CGRectGetHeight(backgroundScrollView.frame))
    }
    
    func navBarHeight() -> CGFloat {
        if let x = navigationController {
            if !x.navigationBarHidden {
                return CGRectGetHeight(x.navigationBar.frame) + 20
            }
        }
        return 0
    }

    func offsetHeight() -> CGFloat {
        return headerHeight + navBarHeight();
    }
    
    func contentView() -> UIScrollView {
        fatalError("Implement me")
    }

    // MARK: UIScrollViewDelegate
    
    func scrollViewDidScroll(scrollView: UIScrollView) {
        var delta: CGFloat = 0
        let rect = CGRectMake(0, 0, CGRectGetWidth(scrollViewContainer.frame), imageHeight)
        let backgroundScrollViewLimit = backgroundScrollView.frame.size.height - offsetHeight()
        
        if scrollView.contentOffset.y < 0.0 {
            //calculate delta
            delta = abs(min(0.0, mainScrollView.contentOffset.y + navBarHeight()))
            backgroundScrollView.frame = CGRectMake(CGRectGetMinX(rect) - delta / 2.0, CGRectGetMinY(rect) - delta,
                CGRectGetWidth(scrollViewContainer.frame) + delta, CGRectGetHeight(rect) + delta)
            floatingHeaderView.alpha = (invisDelta - delta) / invisDelta
        } else {
            delta = mainScrollView.contentOffset.y;
            
            //set alfas
            let newAlpha = 1 - ((blurDistance - delta) / blurDistance);
            blurredImageView.alpha = newAlpha
            floatingHeaderView.alpha = 1
            
            // Here I check whether or not the user has scrolled passed the limit where I want to stick the header, if they have then I move the frame with the scroll view
            // to give it the sticky header look
            if (delta > backgroundScrollViewLimit) {
                backgroundScrollView.frame = CGRect(origin: CGPoint(x: 0, y: delta - backgroundScrollView.frame.size.height + offsetHeight()), size: CGSize(width: CGRectGetWidth(scrollViewContainer.frame), height: imageHeight))
                floatingHeaderView.frame = CGRect(origin: CGPoint(x: 0, y: delta - floatingHeaderView.frame.size.height + offsetHeight()), size: CGSize(width: CGRectGetWidth(scrollViewContainer.frame), height: imageHeight))
                scrollViewContainer.frame = CGRect(origin: CGPoint(x: 0, y: CGRectGetMinY(backgroundScrollView.frame) + CGRectGetHeight(backgroundScrollView.frame)), size: scrollViewContainer.frame.size)
                cv.contentOffset = CGPointMake(0, delta - backgroundScrollViewLimit)
                let contentOffsetY = -backgroundScrollViewLimit * 0.5
                backgroundScrollView.contentOffset = CGPoint(x: 0, y: contentOffsetY)
            } else {
                backgroundScrollView.frame = rect
                floatingHeaderView.frame = rect
                scrollViewContainer.frame = CGRect(origin: CGPoint(x: 0, y: CGRectGetMinY(rect) + CGRectGetHeight(rect)), size: scrollViewContainer.frame.size)
                cv.contentOffset = CGPoint(x: 0, y: 0)
                backgroundScrollView.contentOffset = CGPoint(x: 0, y: -delta * 0.5)
            }
        }
    }
    
    // MARK: Public methods
    
    func setHeaderImage(headerImage: UIImage) {
        headerImageView.image = headerImage
        blurredImageView.image = headerImage //.blurredImageWithRadius(40.0, iterations: 4, tintColor: UIColor.clearColor)
    }
    
    func addHeaderOverlayView(overlayView: UIView) {
        headerOverlayView = overlayView
        floatingHeaderView.addSubview(overlayView)
    }
    
}