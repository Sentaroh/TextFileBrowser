## 1.収集データ
### 1.1.ユーザーからTextFileBrowserに提供されるデータ

ユーザーから提供されるデータは有りません。

### 1.2.TextFileBrowserの活動記録

TextFileBrowserの実行結果の検証と技術サポートのために活動記録データをアプリ内の記憶領域に保存します。
<span style="color: red;"><u>データは”1.3.TextFileBrowser外へのデータの送信または書出し”の操作が無い限り外部に送信されません。</u></span>

- TextFileBrowserのバージョン、TextFileBrowserの実行オプション
- ディレクトリー名、ファイル名、ファイルサイズ、ファイル内容
- デバッグ情報
- エラー情報

### 1.3.TextFileBrowser外へのデータの送信

ユーザーがメニューから「ログの送信」操作しない限りTextFileBrowserが保有するデータは外部に送信できません。

### 1.4.TextFileBrowser内に保存されたデータの削除

TextFileBrowserをアンインストールする事により保存したデータ("1.2.TextFileBrowserの活動記録")はデバイスから削除されます。
<span style="color: red; "><u>ただし、ユーザーの操作により外部ストレージに保存されたデータは削除されません。</u></span>

## 2.アプリ実行に必要な権限

追加の権限は不要です。
