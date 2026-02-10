# コンサート予約システム  
アーキテクチャおよびロジック分析レポート

---

## 1. システムアーキテクチャ概要（System Architecture）

本システムは、大規模トラフィックに対応するコンサート予約サービス向けに設計された**高パフォーマンス・高可用性アーキテクチャ**です。

設計のコアとなるポイントは以下の2点です。

- **待機列（Queue）のRedis移行**
- **トランザクション分離および適切な排他制御**

---

### 技術スタック

- **Application**: Spring Boot 3.x  
- **Database**: PostgreSQL  
  （コア業務データ：Concert / Seat / Reservation / Payment）
- **Queue / Cache**: Redis  
  （待機列管理、Active Token管理）
- **排他制御**
  - Optimistic Lock（残高処理）
  - Pessimistic Lock（座席予約）

---

## 2. End-to-End 全体処理フロー

ユーザーのアクセスから決済完了までのデータフローは以下の通りです。

---

### Step 1: 待機トークン発行（Queue Token Issue）

**User Action**: ユーザーがサービスへアクセス  

#### 処理内容

1. `TokenController` がリクエストを受信
2. `RedisQueueRepository#addToWaitingQueue` を呼び出し
3. Redis の `ZADD` コマンドで `waiting_queue`（Sorted Set）へユーザーIDを追加  
   - Score: 現在のタイムスタンプ
4. 待機順位およびトークンUUIDをレスポンスとして返却

#### なぜRedisか？

RDBへの `INSERT` と比較して、メモリベースの `ZADD` は圧倒的に高速。  
数万規模の同時アクセスでも軽量に処理可能。

---

### Step 2: 待機列アクティベーション（Queue Activation / Scheduling）

**System Action**:  
`QueueScheduler` が定期実行（例：1秒ごと）

#### 処理内容

1. 許容同時接続数（N名）を基準に空きスロットを算出
2. `ZRANGE` で `waiting_queue` から先頭N名を取得
3. 取得ユーザーを `active_tokens`（Hash）へ移動  
   - Key例: `active:token:{uuid}`
4. `waiting_queue` から対象ユーザーを削除

#### 最適化ポイント

大量キー移動時のネットワークオーバーヘッド削減のため、  
**Pipelineによるバッチ処理**を採用。

---

### Step 3: Interceptorによるトークン検証

**User Action**:  
予約可能日取得 / 座席予約 / 決済 などのAPI呼び出し

#### 処理内容

1. `QueueTokenInterceptor` がHTTPヘッダのトークンを検証
2. Resilience設計：
   - 第1段階: RedisでActive状態かを確認
   - 第2段階（Fallback）: Redis障害時はDB（Queueテーブル）を参照
3. 無効トークンや待機状態のトークンは 401 / 403 を返却

Redis障害時でもサービスを停止させない構成。

---

### Step 4: 座席予約（Seat Reservation）

**User Action**:  
希望座席を選択し予約リクエスト送信

#### 処理内容

1. トランザクション開始
2. 対象日付・座席番号に対して
   - **Pessimistic Lock** を取得  
   または  
   - Unique制約により重複予約を防止
3. 座席ステータスを `TEMPORARY_RESERVED` に変更
4. `Reservation` レコード生成
5. 仮押さえ時間（5分）を設定

---

### Step 5: 決済および予約確定（Payment & Confirm）

**User Action**: 決済実行

#### 処理内容

1. 残高取得および減算  
   → **Optimistic Lock（@Version）** による同時更新制御
2. `Payment` レコード保存
3. 予約ステータスを `CONFIRMED` へ変更  
   座席ステータスを `RESERVED` に確定
4. 決済完了イベントを発行し `DataPlatform` へ連携
5. 使用済みトークンをRedisから削除  
   - `active_tokens`
   - `waiting_queue`

次の待機ユーザーへスロットを解放。

---

## 3. Migration分析：JPA（RDB）からRedisへの移行

アーキテクチャ変更の主目的は  
**パフォーマンス向上とスケーラビリティ確保**。

---

### 3.1 従来のJPA（RDB）構成の課題

1. **Polling負荷の増大**  
   「入場可能か？」というリクエストが大量発生し、  
   予約・決済用のDBコネクションプールを圧迫。

2. **ロック競合の増加**  
   待機順管理のためTable Lock / Row Lockが頻発。  
   スループットが大幅に低下。

3. **ORDER BYの高コスト化**  
   数万件を `ORDER BY timestamp` で都度ソートする処理は高負荷。

---

### 3.2 Redis導入効果

1. **O(log N) の順序保証**  
   Sorted Setにより自動ソート。  
   `ZRANK` / `ZRANGE` が高速。

2. **In-Memory処理による高速化**  
   ディスクI/Oが発生せず、応答はミリ秒単位。

3. **TTLによる自動期限管理**  
   ユーザー離脱時も自動Expire。  
   削除バッチ不要。

4. **Active Token検証の高速化**  
   Hashや個別Key参照により O(1) 判定。

---

### 3.3 主なリファクタリングポイント

- `KEYS` → `SCAN` へ変更（ブロッキング回避）
- N+1問題の解消（`HGETALL` や Pipeline活用）
- AOF有効化（`appendonly yes`）によるデータ永続化

---

## 4. 負荷試験結果（Load Test Summary）

### テストシナリオ（LoadTest5）

- 同時ユーザー数：10,000名
- フロー：
  - Queue進入
  - Token有効化待機
  - 残高チャージ
  - 座席予約
  - 決済

---

### 結果

- **Queue / Token**
  - 10,000名 全員正常発行・アクティベート成功（成功率100%）

- **Payment**
  - 予約成功9,074件に対して決済成功率100%

- **データ整合性**
  - 総チャージ金額 ＝ 残高 + 総決済金額  
  → 完全一致

- **補足**
  - 予約成功率が100%でない理由は、  
    テストBotのランダム座席選択による衝突（Collision）。
  - システム不具合ではない。

---

## 5. 結論

Redisによる待機列分離により、  
DB負荷を根本的に遮断。

その結果、コア業務ロジック（予約・決済）は  
DBリソースを十分に活用可能となり、

**大規模トラフィック環境下でも安定したサービス提供が可能であることを実証。**

また、Redis障害時にDBへフォールバックする設計により、  
SPoF（Single Point of Failure）リスクも効果的に緩和している。
