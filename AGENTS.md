# 開発・リリースの進め方

- 誰でも使えるOSSのプラグインとして作る。特定のサーバー（spa77-smpなど）専用のワールド名・文面・設定を入れない。サーバー固有の設定は使う側のリポジトリ（`../spsmc-infra/`）に置く。
- Bolt 1.2.22 が対応する Paper 1.20.6 以降で動くよう、Java 21 と Bukkit API・Bolt API だけを使う。
- コマンドの出力（`BOLT_RESET_OK`・`BOLT_RESET_ERROR ...`）はリセット用スクリプトが読むので、変えるときはREADMEの表も直し、互換が崩れる変更はメジャーバージョンを上げる。
- 作業前に`git status --short`を確認し、既存の変更を上書き・削除しない。
- 変更したら `mvn -B package` と `python3 scripts/test-boltreset-paper.py` を通す。`server-data/` に Paper のJARと `plugins/Bolt-1.2.22.jar` を置く。別の版の確認は `PAPER_JAR=paper-26.3-159.jar python3 scripts/test-boltreset-paper.py`。
- READMEは英語版と日本語版を同じ内容にそろえる。
