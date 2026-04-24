# CustomShop (MVP)

CustomShop is a Paper plugin that loads multiple shops from YAML, supports GUI buy/sell, tracks stock in a persistent file, and applies global inflation multipliers through commands.

## Implemented in this MVP

- Main Menu GUI via `/shop` command with configurable categories (Donut SMP style blocks, farming, ores, combat).
- Multi-shop loading from `config.yml` + `shops/*.yml`
- Exact item placement via `slot: 10` in shop/menu items.
- GUI shop opening with configurable rows/title/item order
- Bulk transactions via Shift-Click mechanics (Buy stack / Sell all capability natively integrated).
- Left-click buy / right-click sell in shop inventories
- Persistent stock state in `plugins/CustomShop/data/stock.yml`
- Persistent inflation state in `plugins/CustomShop/inflation.yml`
- Global restock scheduler + manual restock commands
- Economy abstraction:
  - Full native Vault support out-of-the-box (`softdepend: [Vault]`).
  - Local ledger fallback in `plugins/CustomShop/data/ledger.yml`

## Commands

- `/shop list`
- `/shop info <shop>`
- `/shop open <shop>`
- `/shop reload`
- `/shop restock [shop]`
- `/shop stock <shop> <item> <amount>`
- `/inflate %<change> <sell_multiplier>`
- `/resetinflate`

## Quick build

```powershell
Set-Location "C:\Users\svrce\Desktop\Složky\projekty\mc-pluginy\CustomShop"
cmd /c gradlew.bat compileJava processResources --no-daemon
cmd /c gradlew.bat jar --no-daemon
```

Generated jar (default): `build/libs/CustomShop-1.0.jar`

## Basic run (Paper dev task)

```powershell
Set-Location "C:\Users\svrce\Desktop\Složky\projekty\mc-pluginy\CustomShop"
cmd /c gradlew.bat runServer
```

## Notes

- Full `/shop create`, `/shop delete`, `/shop edit`, and `/shop additem` flow is not implemented yet.
- Per-shop inflation overrides are not implemented yet.
- Pagination/navigation buttons for very large menus are not implemented yet.
