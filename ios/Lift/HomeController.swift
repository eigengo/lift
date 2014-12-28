import Foundation

class CSCell : UICollectionViewCell {
    @IBOutlet var label: UILabel!
}

class HomeController : UICollectionViewController {
    private let sections = [["Arms", "Arms", "Chest", "Arms", "Chest", "Core", "Chest", "Core"]]
    private let headerNib = UINib(nibName: "HomeHeader", bundle: nil)

    override func viewDidLoad() {
        navigationController?.navigationBarHidden = true
        if let x = collectionViewLayout as? CSStickyHeaderFlowLayout {
            x.parallaxHeaderReferenceSize = CGSizeMake(self.view.frame.size.width, 426)
            x.parallaxHeaderMinimumReferenceSize = CGSizeMake(self.view.frame.size.width, 110)
            x.itemSize = CGSizeMake(self.view.frame.size.width, x.itemSize.height)
            x.parallaxHeaderAlwaysOnTop = true
            x.disableStickyHeaders = true
        }
        collectionView?.backgroundColor = UIColor.whiteColor()
        collectionView?.scrollIndicatorInsets = UIEdgeInsetsMake(0, 0, 0, 0)
        collectionView?.registerNib(headerNib, forSupplementaryViewOfKind: CSStickyHeaderParallaxHeader, withReuseIdentifier: "header")
    }
    
    // MARK: UICollectionViewDataSource
    override func numberOfSectionsInCollectionView(collectionView: UICollectionView) -> Int {
        return sections.count
    }
    
    override func collectionView(collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return sections[section].count
    }
    
    override func collectionView(collectionView: UICollectionView, cellForItemAtIndexPath indexPath: NSIndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCellWithReuseIdentifier("cell", forIndexPath: indexPath) as CSCell
        cell.label.text = sections[indexPath.section][indexPath.row]
        return cell
    }
    
    override func collectionView(collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, atIndexPath indexPath: NSIndexPath) -> UICollectionReusableView {
        switch kind {
        case UICollectionElementKindSectionHeader:
            return collectionView.dequeueReusableSupplementaryViewOfKind(kind, withReuseIdentifier: "sectionHeader", forIndexPath: indexPath) as UICollectionReusableView
        case CSStickyHeaderParallaxHeader:
            return collectionView.dequeueReusableSupplementaryViewOfKind(kind, withReuseIdentifier: "header", forIndexPath: indexPath) as UICollectionReusableView
        default: fatalError("Match error")
        }
    }
    
    override func preferredStatusBarStyle() -> UIStatusBarStyle {
        return UIStatusBarStyle.LightContent;
    }
}