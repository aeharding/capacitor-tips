package app.vger.capacitor.plugins.tip;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "CapacitorTips")
public class CapacitorTipsPlugin extends Plugin implements PurchasesUpdatedListener {

    private BillingClient billingClient;
    private PluginCall productsCallback;
    private PluginCall purchaseCallback;

    @Override
    protected void handleOnStart() {
        super.handleOnStart();
        initializeBillingClient();
    }

    private void initializeBillingClient() {
        billingClient = BillingClient.newBuilder(getContext())
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(getLogTag(), "Billing client setup finished successfully");
                } else {
                    Log.e(getLogTag(), "Billing client setup failed with response code: " + billingResult.getResponseCode());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(getLogTag(), "Billing service disconnected, attempting to reconnect...");
                initializeBillingClient();
            }
        });
    }

    private void queryProductDetails(List<String> productIds, OnProductDetailsListener listener) {
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();

        for (String productId : productIds) {
            QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build();

            products.add(product);
        }

        QueryProductDetailsParams.Builder paramsBuilder = QueryProductDetailsParams.newBuilder().setProductList(products);

        billingClient.queryProductDetailsAsync(paramsBuilder.build(), (billingResult, productDetailsListResult) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                listener.onProductDetailsSuccess(productDetailsListResult.getProductDetailsList());
            } else {
                listener.onProductDetailsFailure();
            }
        });
    }

    // Method to handle Capacitor call for listing products
    @PluginMethod
    public void listProducts(PluginCall call) {
        productsCallback = call;

        List<String> productIds;

        try {
            productIds = call.getArray("productIdentifiers").toList();
        } catch (JSONException err) {
            call.reject("Failed to parse productIdentifiers");
            return;
        }

        if (productIds == null || productIds.isEmpty()) {
            call.reject("Missing or invalid 'productIdentifiers' parameter");
            return;
        }

        queryProductDetails(productIds, new OnProductDetailsListener() {
            @Override
            public void onProductDetailsSuccess(List<ProductDetails> productDetailsList) {
                var jsonProducts = new JSONArray();

                for (ProductDetails details : productDetailsList) {
                    var offerDetails = details.getOneTimePurchaseOfferDetails();

                    if (offerDetails == null) continue;

                    JSObject product = new JSObject();

                    product.put("identifier", details.getProductId());
                    product.put("priceString", offerDetails.getFormattedPrice());
                    product.put("price", offerDetails.getPriceAmountMicros() / 1000000.0); // Convert to dollars
                    product.put("currencyCode", offerDetails.getPriceCurrencyCode());
                    product.put("description", details.getDescription());
                    product.put("name", details.getTitle());

                    jsonProducts.put(product);
                }

                productsCallback.resolve(new JSObject().put("products", jsonProducts));
            }

            @Override
            public void onProductDetailsFailure() {
                productsCallback.reject("Failed to retrieve product details");
            }
        });
    }

    // Interface for the listener to handle product details retrieval callbacks
    private interface OnProductDetailsListener {
        void onProductDetailsSuccess(List<ProductDetails> productDetailsList);

        void onProductDetailsFailure();
    }

    @PluginMethod
    public void purchaseProduct(PluginCall call) {
        purchaseCallback = call;

        String identifier = call.getString("identifier");
        if (identifier == null || identifier.isEmpty()) {
            call.reject("Missing or invalid 'identifier' parameter");
            return;
        }

        // Call the method to retrieve product details
        queryProductDetails(List.of(identifier), new OnProductDetailsListener() {
            @Override
            public void onProductDetailsSuccess(List<ProductDetails> productDetailsList) {
                // Find the product details by ID
                ProductDetails productDetails = getFirstProductDetailsByProductId(productDetailsList, identifier);

                if (productDetails == null) {
                    call.reject("Product not found");
                    return;
                }

                // Use the retrieved product details for the purchase flow
                List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                        List.of(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .build()
                        );

                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build();

                billingClient.launchBillingFlow(getActivity(), billingFlowParams);
            }

            @Override
            public void onProductDetailsFailure() {
                call.reject("Failed to retrieve product details");
            }
        });
    }

    private static ProductDetails getFirstProductDetailsByProductId(List<ProductDetails> productDetailsList, String productId) {
        for (ProductDetails productDetails : productDetailsList) {
            if (productDetails.getProductId().equals(productId)) {
                return productDetails; // Found a match, return it
            }
        }

        return null; // No match found
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            // Process the purchased items
            for (Purchase purchase : purchases) {
                consumePurchase(purchase);

                // Handle the purchased item
                if (purchaseCallback != null) {
                    purchaseCallback.resolve();
                    purchaseCallback = null;
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            if (purchaseCallback != null) {
                purchaseCallback.reject("Purchase canceled", "cancelled");
                purchaseCallback = null;
            }
        } else {
            if (purchaseCallback != null) {
                purchaseCallback.reject("Failed to complete the purchase", "failed");
                purchaseCallback = null;
            }
        }
    }

    private void consumePurchase(Purchase purchase) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.consumeAsync(consumeParams, (billingResult, s) -> {
            Log.d(getLogTag(), "Purchase consumed");
        });
    }
}
