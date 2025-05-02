# capacitor-tips

Add tips to your capacitor app via in-app purchases

## Install

```bash
npm install capacitor-tips
npx cap sync
```

## API

<docgen-index>

* [`listProducts(...)`](#listproducts)
* [`purchaseProduct(...)`](#purchaseproduct)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### listProducts(...)

```typescript
listProducts(options: { productIdentifiers: string[]; }) => Promise<{ products: Product[]; }>
```

| Param         | Type                                           |
| ------------- | ---------------------------------------------- |
| **`options`** | <code>{ productIdentifiers: string[]; }</code> |

**Returns:** <code>Promise&lt;{ products: Product[]; }&gt;</code>

--------------------


### purchaseProduct(...)

```typescript
purchaseProduct(product: Product) => Promise<void>
```

| Param         | Type                                        |
| ------------- | ------------------------------------------- |
| **`product`** | <code><a href="#product">Product</a></code> |

--------------------


### Interfaces


#### Product

| Prop               | Type                | Description                       |
| ------------------ | ------------------- | --------------------------------- |
| **`identifier`**   | <code>string</code> |                                   |
| **`priceString`**  | <code>string</code> | Locale formatted price ex `$3.99` |
| **`price`**        | <code>number</code> | ex. `3.99`                        |
| **`currencyCode`** | <code>string</code> | ex. `USD`                         |
| **`description`**  | <code>string</code> |                                   |
| **`name`**         | <code>string</code> |                                   |

</docgen-api>
