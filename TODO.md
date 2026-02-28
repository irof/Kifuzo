# Kifuzo Project TODO / Technical Debt

## 技術的負債 (Technical Debt)
- [x] **インポートの完全な最適化**: 手動確認により不要なインポートやフルパス指定を整理済み。
- [x] **CsaParser / KifuParser のクラス化**: KifuFormatParser インターフェースを定義し、KifParser と CsaParser をクラスとして実装。状態管理のカプセル化と拡張性を向上済み。

## 一時的な対応 (Temporary Workarounds)
- [ ] **盤面図パースの網羅性**: `HeaderParserKif.kt` で 1 文字ずつの走査によるパースを導入した。現在のテストケース（提示された局面図）では動作しているが、成駒（2文字）が混在するケースなど、より多様な KIF 形式に対する堅牢性の検証とテスト追加が必要。

## 将来の改善 (Future Improvements)
- [ ] **カバレッジのさらなる向上**: Models/Logic のラインカバレッジを 90% 以上、Instruction カバレッジを 85% 以上に引き上げる。
- [ ] **UI テストの導入**: ViewModel のテストは強化されたが、Compose UI 自体の表示やインタラクションのテストが未着手。
