# BoltReset

[English](README.md)

ワールドを作り直したあとに、そのワールドに残った[Bolt](https://modrinth.com/plugin/bolt)のブロック保護を消すPaperプラグインです。

資源ワールドを消して作り直しても、Boltは同じワールド名の古い保護を持ち続けます。そのままだと、新しいワールドのチェストやドアが、前のワールドの保護でロックされてしまいます。BoltResetは、ワールドが本当に作り直されたことを確かめてから、そのワールドのブロック保護をコンソールからすべて消します。

- コンソールとRCONからだけ実行できます。プレイヤーは使えません。
- `config.yml`に書いたワールドだけが対象です。
- ワールドのUIDが指定した新しい値に変わっていないと断るので、作り直していないワールドを消すことはありません。
- 既定では、ホワイトリストがオンで、だれも接続していないときだけ実行します。
- 結果を機械で読みやすい1行（`BOLT_RESET_OK ...`または`BOLT_RESET_ERROR ...`）で返すので、リセット用のスクリプトから確かめられます。
- エンティティの保護と、Boltのプレイヤーごとの共有設定には触れません。

## 動作環境

- Paper 1.20.6以降（Paper 26.2・26.3で動作確認）
- Java 21以降（サーバーのバージョンが要求するもの）
- [Bolt](https://modrinth.com/plugin/bolt) 1.2.22以降

## 導入手順

1. [Releases](https://github.com/spa77k/BoltReset/releases)から`bolt-reset-1.0.0.jar`をダウンロードします。
2. Boltと同じ`plugins/`フォルダーに置きます。
3. サーバーを再起動します。`plugins/BoltReset/config.yml`ができます。
4. 作り直すワールドを`worlds`に書き、サーバーを再起動します。

## コマンド

サーバーのコンソールかRCONから実行します。

| コマンド | 内容 |
| --- | --- |
| `boltreset status` | Boltが使えるとき`BOLT_RESET_READY`を返す |
| `boltreset clear <ワールド> <旧UID> <新UID>` | `<ワールド>`のブロック保護をすべて消す |

`<旧UID>`は作り直す前のワールドのUID、`<新UID>`は作り直したあとのUIDです。ワールドを消す前に旧UID（ワールドフォルダーの`uid.dat`など）を控えておき、新しいワールドを読み込んだあとに両方を渡します。

### 返す結果

| 結果 | 意味 |
| --- | --- |
| `BOLT_RESET_OK world=<ワールド> removed=<件数> remaining=0` | 消して、残りが0件であることを確かめた |
| `BOLT_RESET_ERROR world-not-allowed` | `worlds`に書かれていないワールド |
| `BOLT_RESET_ERROR world-not-regenerated` | 2つのUIDが同じか、読み込まれているワールドのUIDが`<新UID>`と違う |
| `BOLT_RESET_ERROR players-not-blocked` | ホワイトリストがオフか、接続している人がいる |
| `BOLT_RESET_ERROR console-only` | プレイヤーが実行した |
| `BOLT_RESET_ERROR Bolt-unavailable` | Boltが有効になっていない |
| `BOLT_RESET_ERROR remaining=<件数>` | 消したあとも保護が残っていた |

同じワールドにもう一度`clear`を実行すると、`removed=0`で成功します。

## 設定

`plugins/BoltReset/config.yml`

| キー | 既定値 | 内容 |
| --- | --- | --- |
| `worlds` | `[]` | 保護を消してよいワールド。空のあいだは、すべて断ります。 |
| `require-whitelist` | `true` | ホワイトリストがオフなら断る。 |
| `require-empty-server` | `true` | 接続している人がいれば断る。 |

設定の変更はサーバーの再起動で反映されます。

## ライセンス

[MIT](LICENSE)
