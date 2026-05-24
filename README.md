# DailySchedule-API

DailySchedule-API は、既存の `DailySchedule` https://github.com/SK-Shun/DailySchedule をもとに、画面表示中心のWebアプリケーションから REST API として利用できる形に再設計したスケジュール管理APIです。

元の `DailySchedule` では、Thymeleaf を使った画面表示、フォーム送信、画面上でのエラー表示を中心に実装していました。  

この `DailySchedule-API` では、同じスケジュール管理ドメインを維持しつつ、JSONリクエスト・JSONレスポンスで操作できる REST API として再構成しています。

---

# 元の DailySchedule から変更した点

## 1. 画面中心のMVCからREST API中心へ変更

元の `DailySchedule` は、以下のような画面遷移を前提にした構成でした。

```text
Controller
  ↓
Service
  ↓
Repository
  ↓
Database
```

`DailySchedule-API` では、Controller を REST API 用に分離し、`@RestController` を使ってJSONを返す構成に変更しています。

```text
Client / Postman / Frontend
  ↓ HTTP Request(JSON)
REST API Controller
  ↓
Service
  ↓
Repository
  ↓
Database
  ↑
HTTP Response(JSON)
```

これにより、Thymeleaf画面に依存せず、Postman・JavaScriptフロントエンド・スマートフォンアプリなど、さまざまなクライアントから利用できるAPIになっています。

---

## 2. `controller/api` パッケージを追加

API版では、画面用Controllerではなく、REST API用Controllerを配置しています。

```text
controller
└─ api
   ├─ ScheduleSheetApiController.java
   └─ ScheduleEntryApiController.java
```

主な役割は以下です。

- HTTPリクエストを受け取る
- リクエストDTOを受け取る
- Service層へ処理を委譲する
- 処理結果をDTOとしてJSONで返す

Controllerでは業務ロジックを持たず、Service層に処理を任せる構成にしています。

---

## 3. リクエストDTOを追加

APIではHTMLフォームではなくJSONを受け取るため、API用のリクエストDTOを追加しています。

```text
api
└─ request
   ├─ CreateScheduleSheetRequest.java
   └─ CreateScheduleEntryRequest.java
```

例：

```json
{
  "title": "Study Plan"
}
```

```json
{
  "type": "WORK",
  "startMin": 540,
  "endMin": 600,
  "memo": "Spring Boot study"
}
```

EntityをControllerで直接受け取らず、Request DTOを通して入力値を受け取ることで、Web/API層と永続化層を分離しています。

---

## 4. レスポンスDTOを追加

APIのレスポンス形式を統一するため、レスポンスDTOを追加しています。

```text
api
└─ response
   ├─ ApiErrorResponse.java
   └─ ValidationErrorResponse.java
```

通常のデータ取得・作成ではDTOを返し、エラー時には以下のようなJSONを返します。

```json
{
  "timestamp": "2026-05-21T16:15:41.1091387",
  "status": 404,
  "error": "Schedule Sheet Not Found",
  "message": "スケジュールシートが見つかりません: study-plan",
  "path": "/api/schedule-sheets/study-plan"
}
```

画面にエラーメッセージを表示するのではなく、API利用者が扱いやすいJSON形式でエラー情報を返すように変更しました。

---

## 5. API用の例外ハンドリングを追加

元の `DailySchedule` では、画面を再表示してエラーメッセージを表示する設計でした。

API版では、`@RestControllerAdvice` を使い、例外発生時にJSONレスポンスを返すように変更しています。

```text
advice
└─ ApiExceptionHandler.java
```

対応している主な例外は以下です。

- ScheduleSheetNotFoundException
- ScheduleEntryNotFoundException
- ScheduleConflictException
- MethodArgumentNotValidException

これにより、API利用者はHTTPステータスコードとJSONレスポンスからエラー内容を判断できます。

---

## 6. バリデーションをAPI向けに変更

API版では、`jakarta.validation` を使ってリクエストDTOに入力制約を設定しています。

例：

- タイトル未入力
- 開始時刻が範囲外
- 終了時刻が範囲外
- memoの文字数超過
- type未指定

入力値に問題がある場合は、400 Bad Request としてバリデーションエラーをJSONで返します。

---

## 7. Swagger / OpenAPI を追加

API仕様を確認しやすくするため、`springdoc-openapi` を追加しています。

これにより、APIエンドポイントの確認や簡易的な実行テストをブラウザ上で行えます。

```text
/swagger-ui/index.html
```

APIとして公開・説明しやすい構成にするための変更です。

---

## 8. `config` パッケージを追加

API版では、設定系のクラスを配置するために `config` パッケージを追加しています。

```text
config
```

主に以下のような用途を想定しています。

- OpenAPI / Swagger 設定
- JSONシリアライズ設定
- Web設定
- 将来的なセキュリティ設定

アプリケーションが大きくなっても、設定クラスが他の層に混ざらないように分離しています。

---

## 9. テスト構成をAPI向けに変更

API版では、画面表示のテストではなく、JSON APIのテストを中心にしています。

```text
src/test/java/com/example/demo
├─ advice
├─ controller
│  └─ api
├─ integration
├─ repository
└─ service
```

特にAPI Controllerのテストでは、MockMvcを使って以下を検証しています。

- HTTPステータスコード
- JSONレスポンス
- バリデーションエラー
- 例外発生時のレスポンス形式

また、統合テストでは、APIの一連の流れを確認できるようにしています。

---

# 使用技術

| 分類 | 技術 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 |
| Web | Spring MVC / REST API |
| ORM | Spring Data JPA |
| Database | PostgreSQL |
| Migration | Flyway |
| Validation | Jakarta Validation |
| API Docs | springdoc-openapi / Swagger UI |
| Test | JUnit 5 / Mockito / MockMvc / Testcontainers |
| Build | Maven |

---

# 主な機能

- スケジュールシート作成API
- スケジュールシート一覧取得API
- スケジュールシート詳細取得API
- 予定登録API
- 予定削除API
- slug形式URL
- 同一TaskType内の時間重複チェック
- DTOによるAPI入出力の分離
- バリデーションエラーのJSONレスポンス
- 例外発生時のJSONレスポンス
- FlywayによるDBマイグレーション
- PostgreSQL + Testcontainers による統合テスト
- Swagger UI によるAPI仕様確認

---

# API設計

## Schedule Sheet API

| Method | URL | 説明 |
|---|---|---|
| GET | `/api/schedule-sheets` | スケジュールシート一覧取得 |
| POST | `/api/schedule-sheets` | スケジュールシート作成 |
| GET | `/api/schedule-sheets/{slug}` | スケジュールシート詳細取得 |
| DELETE | `/api/schedule-sheets/{slug}` | スケジュールシート削除 |

## Schedule Entry API

| Method | URL | 説明 |
|---|---|---|
| POST | `/api/schedule-sheets/{slug}/entries` | 予定登録 |
| DELETE | `/api/schedule-sheets/{slug}/entries/{entryId}` | 予定削除 |

---

# アーキテクチャ

```text
Client
  ↓
Controller / API
  ↓
Request DTO
  ↓
Service
  ↓
Repository
  ↓
Entity
  ↓
Database
```

読み取り時は、Entityをそのまま返さずDTOに変換して返します。

```text
Database
  ↓
Entity
  ↓
Service / Query Service
  ↓
DTO
  ↓
JSON Response
```

---

# パッケージ構成

```text
src/main/java/com/example/demo
├─ advice
├─ api
│  ├─ request
│  └─ response
├─ config
├─ controller
│  └─ api
├─ dto
├─ entity
├─ repository
└─ service
```

---

# Entity設計

## ScheduleSheet

スケジュール全体を管理するシートです。

| カラム | 内容 |
|---|---|
| id | UUID |
| title | シート名 |
| slug | URL識別子 |
| createdAt | 作成日時 |

---

## ScheduleEntry

個別の予定データです。

| カラム | 内容 |
|---|---|
| id | UUID |
| sheet | 所属するScheduleSheet |
| type | TaskType |
| startMin | 開始時刻 |
| endMin | 終了時刻 |
| memo | メモ |
| createdAt | 作成日時 |

---

# TaskType

予定種別は enum で管理しています。

```text
WORK
HOBBY
BREAK
HOUSEWORK
LIFE
FREE
```

---

# 時間管理の設計

時刻は `LocalTime` ではなく、分単位の整数で管理しています。

例：

| 表示時刻 | 保存値 |
|---|---|
| 09:00 | 540 |
| 10:00 | 600 |
| 24:00 | 1440 |

この設計により、`24:00` の扱いをシンプルにし、時間帯の重複判定も数値比較で行えるようにしています。

---

# 時間重複チェック

同一TaskType内で、時間帯が重複しないようにしています。

判定条件：

```text
new.start < existing.end
AND
new.end > existing.start
```

この判定により、以下のような重複を検出できます。

```text
既存: 09:00 - 10:00
新規: 09:30 - 10:30
```

一方で、以下は重複として扱いません。

```text
既存: 09:00 - 10:00
新規: 10:00 - 11:00
```

---

# エラーレスポンス設計

API版では、例外発生時にHTML画面ではなくJSONを返します。

## 404 Not Found

```json
{
  "timestamp": "2026-05-21T16:15:41.1091387",
  "status": 404,
  "error": "Schedule Sheet Not Found",
  "message": "スケジュールシートが見つかりません: study-plan",
  "path": "/api/schedule-sheets/study-plan"
}
```

## 409 Conflict

```json
{
  "timestamp": "2026-05-21T16:15:41.1091387",
  "status": 409,
  "error": "Schedule Conflict",
  "message": "同じ種別の予定が既に登録されています",
  "path": "/api/schedule-sheets/study-plan/entries"
}
```

## 400 Bad Request

```json
{
  "timestamp": "2026-05-21T16:15:41.1091387",
  "status": 400,
  "error": "Validation Error",
  "message": "入力値が不正です",
  "path": "/api/schedule-sheets",
  "errors": [
    {
      "field": "title",
      "message": "タイトルは必須です"
    }
  ]
}
```

---

# テスト

API版では、以下の観点でテストを作成しています。

## Controller Test

- REST APIのHTTPステータス確認
- JSONレスポンス確認
- バリデーションエラー確認
- Service層が適切に呼ばれるか確認

## Advice Test

- 例外発生時のHTTPステータス確認
- エラーレスポンスJSONの形式確認
- バリデーションエラーのレスポンス確認

## Service Test

- スケジュールシート作成
- slug生成
- 予定登録
- 時間重複チェック
- 存在しないデータへの例外処理

## Repository Test

- PostgreSQL上でのRepository動作確認
- slug検索
- exists系メソッド
- 削除処理
- 重複チェック用クエリ

## Integration Test

- APIを通した一連の操作確認
- シート作成
- 予定登録
- 詳細取得
- 削除
- エラーケース確認

---

# このAPI版で意識したこと

## 1. Entityを直接APIに出さない

EntityはDB永続化のためのオブジェクトであり、APIレスポンスとして直接返さないようにしています。

その代わり、DTOを使ってAPIの入出力を明確に分離しています。

これにより、DB設計の変更がAPI仕様に直接影響しにくくなります。

---

## 2. Controllerに業務ロジックを書かない

ControllerはHTTPリクエストとレスポンスの制御に集中させ、予定登録・slug生成・重複チェックなどの業務ロジックはService層に配置しています。

---

## 3. 例外処理を共通化する

各Controllerでtry-catchを書くのではなく、`ApiExceptionHandler` に例外処理を集約しています。

これにより、エラーレスポンスの形式を統一し、Controllerの責務を小さく保っています。

---

## 4. API利用者にとって分かりやすいレスポンスを返す

APIでは、単にエラーを返すだけでなく、以下の情報を含めるようにしています。

- timestamp
- status
- error
- message
- path
- validation errors

これにより、フロントエンドや外部クライアント側でエラー内容を扱いやすくしています。

---

# DailySchedule との比較まとめ

| 項目 | DailySchedule | DailySchedule-API |
|---|---|---|
| 目的 | 画面付きWebアプリ | REST API |
| 表示 | Thymeleaf | JSON |
| 入力 | HTMLフォーム | JSON Request |
| Controller | 画面遷移用Controller | API用Controller |
| エラー処理 | 画面にメッセージ表示 | JSONエラーレスポンス |
| DTO | ViewModel中心 | Request / Response DTO中心 |
| テスト | MVC・Service・Repository中心 | API Controller・Advice・Integration中心 |
| API仕様確認 | なし | Swagger UI |
| 利用想定 | ブラウザ操作 | Postman / フロントエンド / 外部クライアント |

---

# まとめ

`DailySchedule-API` は、既存の `DailySchedule` のドメイン設計を活かしながら、REST APIとして再設計したプロジェクトです。

画面中心のMVCアプリから、JSONベースで操作できるAPIへ変更することで、フロントエンド分離・外部連携・モバイルアプリ連携を想定した構成にしています。

特に、Controller / Service / Repository の責務分離、DTOによる入出力分離、共通例外ハンドリング、バリデーション、統合テストを意識して実装しています。
