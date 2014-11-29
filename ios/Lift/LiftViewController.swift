import Foundation

extension UIViewController {
    
    func setBackgroundImage(imageName: String) {
        let backgroundImage = UIImageView(image: UIImage(named: imageName))
        backgroundImage.alpha = 0.3
        backgroundImage.contentMode = UIViewContentMode.ScaleAspectFill
        backgroundImage.clipsToBounds = true
        backgroundImage.frame = self.view.frame
        self.view.addSubview(backgroundImage)
        self.view.sendSubviewToBack(backgroundImage)
    }
    
}
