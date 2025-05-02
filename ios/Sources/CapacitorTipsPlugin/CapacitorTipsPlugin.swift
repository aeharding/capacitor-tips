import Capacitor
/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
import Foundation
import StoreKit

@objc(CapacitorTipsPlugin)
public class CapacitorTipsPlugin: CAPPlugin, SKProductsRequestDelegate, SKPaymentTransactionObserver, CAPBridgedPlugin {
    public let identifier = "CapacitorTipsPlugin" 
    public let jsName = "CapacitorTips" 
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "listProducts", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "purchaseProduct", returnType: CAPPluginReturnPromise),
    ] 
  var productsRequest: SKProductsRequest?
  var productsCallback: CAPPluginCall?

  var paymentQueue: SKPaymentQueue?
  var purchaseCallback: CAPPluginCall?

  public override func load() {
    SKPaymentQueue.default().add(self)
  }

  @objc func listProducts(_ call: CAPPluginCall) {
    guard let productIds = call.getArray("productIdentifiers", String.self) else {
      call.reject("Missing or invalid 'productIdentifiers' parameter")
      return
    }

    productsCallback = call
    let productIdentifiers = Set(productIds)
    productsRequest = SKProductsRequest(productIdentifiers: productIdentifiers)
    productsRequest?.delegate = self
    productsRequest?.start()
  }

  public func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
    var products = [[String: Any]]()

    for product in response.products {
      let priceFormatter = NumberFormatter()
      priceFormatter.numberStyle = .currency
      priceFormatter.locale = product.priceLocale
      let priceString = priceFormatter.string(from: product.price)!

      products.append([
        "identifier": product.productIdentifier,
        "priceString": priceString,
        "price": product.price,
        "currencyCode": product.priceLocale.currencyCode ?? "",
        "description": product.localizedDescription,
        "name": product.localizedTitle
      ])
    }

    productsCallback?.resolve(["products": products])
    productsCallback = nil
  }

  @objc func purchaseProduct(_ call: CAPPluginCall) {
    guard purchaseCallback == nil else {
      call.reject("Only one purchase at a time, please.")
      return
    }
    guard let identifier = call.getString("identifier") else {
      call.reject("Missing or invalid 'identifier' parameter")
      return
    }

    let paymentRequest = SKMutablePayment()
    paymentRequest.productIdentifier = identifier

    if SKPaymentQueue.canMakePayments() {
      SKPaymentQueue.default().add(paymentRequest)
      purchaseCallback = call
    } else {
      call.reject("User is not allowed to make payments")
    }
  }

  public func paymentQueue(
    _ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]
  ) {
    for transaction in transactions {
      switch transaction.transactionState {
      case .purchased:
        SKPaymentQueue.default().finishTransaction(transaction)
        purchaseCallback?.resolve()
        purchaseCallback = nil

      case .failed:
        // Handle the failure
        if let error = transaction.error {
          print("Transaction failed with error: \(error.localizedDescription)")
        }

        SKPaymentQueue.default().finishTransaction(transaction)

        if let error = transaction.error as? NSError {
          if error.domain == SKErrorDomain {
            switch error.code {
            case SKError.clientInvalid.rawValue:
              purchaseCallback?.reject(
                "client is not allowed to issue the request", "clientInvalid")
              purchaseCallback = nil
            case SKError.paymentCancelled.rawValue:
              purchaseCallback?.reject("Payment cancelled", "cancelled")
              purchaseCallback = nil
            case SKError.paymentInvalid.rawValue:
              purchaseCallback?.reject("purchase identifier was invalid", "paymentInvalid")
              purchaseCallback = nil
            case SKError.paymentNotAllowed.rawValue:
              purchaseCallback?.reject(
                "this device is not allowed to make the payment", "paymentNotAllowed")
              purchaseCallback = nil
            default:
                break;
            }
          }
        }

        purchaseCallback?.reject("Unknown error", "unknown")
        purchaseCallback = nil

      case .restored:
        // Handle restored purchases (if applicable)
        SKPaymentQueue.default().finishTransaction(transaction)

      default:
        break
      }
    }
  }
}
