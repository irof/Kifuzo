# Kifuzo Project TODO / Technical Debt

## 技術的負債 (Technical Debt)
- [x] **インポートの完全な最適化**: 手動確認により不要なインポートやフルパス指定を整理済み。
- [x] **CsaParser / KifuParser のクラス化**: KifuFormatParser インターフェースを定義し、KifParser と CsaParser をクラスとして実装。状態管理のカプセル化と拡張性を向上済み。

## 一時的な対応 (Temporary Workarounds)
- [x] **盤面図パースの網羅性**: `KifHeaderParser.kt` で 2 文字ずつの固定幅によるパースを実装し、成駒（2文字）が混在するケースなど、多様な KIF 形式に対する堅牢性を向上させた。テストコードによる検証も完了。

## 将来の改善 (Future Improvements)
- [ ] **カバレッジのさらなる向上**: Models/Logic のラインカバレッジを 90% 以上、Instruction カバレッジを 85% 以上に引き上げる。(Models は 95% 以上達成、Logic は約 81.5% まで向上済み)
- [ ] **UI テストの導入**: ViewModel のテストは強化されたが、Compose UI 自体の表示やインタラクションのテストが未着手。
