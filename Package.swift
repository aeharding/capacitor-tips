// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorTips",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorTips",
            targets: ["CapacitorTipsPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "CapacitorTipsPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/CapacitorTipsPlugin"),
        .testTarget(
            name: "CapacitorTipsPluginTests",
            dependencies: ["CapacitorTipsPlugin"],
            path: "ios/Tests/CapacitorTipsPluginTests")
    ]
)