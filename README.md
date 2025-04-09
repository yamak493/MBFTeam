# 【Folia対応】MBFTeam

MBFTeamプラグインでは、チーム管理やゲーム進行を簡単に行うためのコマンドを提供します。以下は各コマンドの説明です。

---

## **/mbfteam コマンド**

### **権限**
- このコマンドを使用するには、`mbfteam.admin` 権限が必要です。

### **基本構文**
```
/mbfteam <サブコマンド> [引数]
```

---

### **サブコマンド一覧**

#### **1. /mbfteam join <プレイヤー名> <チーム名>**
- 指定したプレイヤーを指定したチームに参加させます。

##### **使い方**
```
/mbfteam join <プレイヤー名> <チーム名>
```

##### **例**
```
/mbfteam join Steve red
```

##### **注意**
- チーム名は以下のいずれかを指定してください: `red`, `blue`, `green`, `yellow`, `purple`
- プレイヤーがすでに他のチームに所属している場合、自動的にそのチームから削除されます。

---

#### **2. /mbfteam leave**
- 全プレイヤーをすべてのチームから脱退させます。

##### **使い方**
```
/mbfteam leave
```

##### **効果**
- 全プレイヤーのチーム情報をリセットします。

---

#### **3. /mbfteam tp <チーム名>**
- 指定したチームのオンラインプレイヤーを、コマンド実行者の現在地にテレポートさせます。

##### **使い方**
```
/mbfteam tp <チーム名>
```

##### **例**
```
/mbfteam tp blue
```

##### **注意**
- チーム名は以下のいずれかを指定してください: `red`, `blue`, `green`, `yellow`, `purple`
- このコマンドはプレイヤーのみが実行可能です。

---

#### **4. /mbfteam start**
- ゲームを開始します。全プレイヤーに通知を行い、チームカラーに応じた色付き革チェストプレートを配布します。

##### **使い方**
```
/mbfteam start
```

##### **効果**
- 全プレイヤーにゲーム開始のタイトルを表示します。
- チームカラーに応じた革チェストプレートを配布します。

---

#### **5. /mbfteam end**
- ゲームを終了します。全プレイヤーに通知を行い、革チェストプレートを削除します。

##### **使い方**
```
/mbfteam end
```

##### **効果**
- 全プレイヤーにゲーム終了のタイトルを表示します。
- 革チェストプレートをインベントリから削除します。

---

### **補足情報**

- **フレンドリーファイアの無効化**
  - 同じチーム内のプレイヤー同士での攻撃は自動的にキャンセルされます。
  - 攻撃を試みた場合、攻撃者に警告メッセージが表示されます。

- **有効なチーム名**
  - `red`, `blue`, `green`, `yellow`, `purple`

---

このプラグインを使用して、チームベースのゲームを簡単に管理しましょう！
