import Foundation

@objc public class CapacitorTips: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
