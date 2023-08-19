package com.example.myapplication20230129;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    //タグの定義
    private static final String DEBUG_TAG = "Rakuten";

    //基本URL（フォーマットバージョン=2 ⇐ 楽天APIオプション一覧を参照してください。)
    private static final String URL = "https://app.rakuten.co.jp/services/api/IchibaItem/Search/20170706?applicationId=1077328739738883157&formatVersion=2&";

    //定義
    private EditText editText;
    private Button searchButton;

    //絞り込み設定、前ページ、次ページのボタン変数
    private Button narrowingDownButton;
    /*
    private Button previousPage;
    private Button nextPage;
    */
    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //変数とidの関連付け
        editText = (EditText) findViewById(R.id.searchText);
        searchButton = (Button) findViewById(R.id.searchButton);

        //絞り込み設定、前ページ、次ページのボタン
        narrowingDownButton = (Button) findViewById(R.id.narrowingDownButton);
        /*
        previousPage = (Button) findViewById(R.id.previousPage);
        nextPage = (Button) findViewById(R.id.nextPage);
        */
        textView = (TextView) findViewById(R.id.textView);

        //検索ボタンが押されたら
        searchButton.setOnClickListener(view -> {
            String text = editText.getText().toString();
            if (!text.equals("")) {
                String urlFull = URL + "keyword=" + text;
                searchProductInfo(urlFull);
            }
        });

        //絞り込み設定のボタンが押されたら
        narrowingDownButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SubActivity.class);
            intent.putExtra("URL",URL);
            startActivity(intent);
        });
    }



    //検索用URLを取得後の処理
    @UiThread
    private void searchProductInfo(final String urlFull) {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);
        searchProductInfoBackgroundReceiver backgroundReceiver = new searchProductInfoBackgroundReceiver(handler, urlFull);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundReceiver);
    }



    //非同期通信
    private class searchProductInfoBackgroundReceiver implements Runnable {
        //ハンドラオブジェクト
        private final Handler _handler;
        //商品情報取得用URL
        private final String _urlFull;

        //コンストラクタ
        public searchProductInfoBackgroundReceiver(Handler handler, String urlFull) {
            _handler = handler;
            _urlFull = urlFull;
        }

        @WorkerThread
        @Override
        public void run() {

            HttpURLConnection con = null;

            InputStream is = null;

            String result = "";

            try {
                //URLオブジェクトの生成
                URL url = new URL(_urlFull);
                //URLオブジェクトからHttpURLConnectionオブジェクトを取得
                con = (HttpURLConnection) url.openConnection();
                //接続までの使用時間
                con.setConnectTimeout(1000);
                //データ取得までの使用時間
                con.setReadTimeout(1000);
                //HTTP接続メソッドをGET設定
                con.setRequestMethod("GET");
                //接続
                con.connect();
                //HttpURLConnectionオブジェクトからレスポンスデータを取得
                is = con.getInputStream();
                //レスポンスデータ(InputStreamオブジェクト)を文字列に変換
                result = is2String(is);
            }
            catch (MalformedURLException ex) {
                Log.e(DEBUG_TAG, "URL変換失敗");
            }
            catch (SocketTimeoutException ex) {
                Log.w(DEBUG_TAG, "通信タイムアウト", ex);
            }
            catch (IOException ex) {
                Log.e(DEBUG_TAG, "通信失敗", ex);
            }
            finally {
                //HttpURLConnectionオブジェクトがnullでない場合
                if(con != null){
                    con.disconnect();
                }
                //InputStreamオブジェクトがnullでない場合
                if(is != null) {
                    try {
                        is.close();
                    }
                    catch (IOException ex) {
                        Log.e(DEBUG_TAG, "InputStream開放失敗", ex);
                    }
                }
            }
            searchProductInfoPostExecutor postExecutor = new searchProductInfoPostExecutor(result);
            _handler.post(postExecutor);
        }


        private String is2String(InputStream is) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuffer sb = new StringBuffer();
            char[] b = new char[1024];
            int line;
            while(0 <= (line = reader.read(b))) {
                sb.append(b, 0, line);
            }
            return  sb.toString();
        }
    }



    //JSONの解析
    private class  searchProductInfoPostExecutor implements Runnable {
        //取得したデータJSON文字列
        private final String _result;

        //コンストラクタ
        public searchProductInfoPostExecutor(String result) {
            _result = result;
        }

        @UiThread
        @Override
        public void run() {
            //商品名格納変数
            String itemName = "";
            //商品価格格納変数
            String itemPrice = "";
            //デバッグ用変数
            String log = "";

            try {
                //ルートJSONオブジェクトを生成
                JSONObject rootJSON = new JSONObject(_result);
                //配列Itemsを取得
                JSONArray ItemsArrayJSON = rootJSON.getJSONArray("Items");
                //配列Itemsの0番目を参照
                JSONObject ItemsJSON = ItemsArrayJSON.getJSONObject(0);
                //ItemsJSON内のキーであるitemNameの内容を取得
                itemName = ItemsJSON.getString("itemName");
                //ItemsJSON内のキーであるitemPriceの内容を取得
                itemPrice = ItemsJSON.getString("itemPrice");

            }
            catch (JSONException ex) {
                Log.w(DEBUG_TAG, "JSON解析失敗", ex);
                log = "JSON解析失敗\n" + ex;
            }
            String text =  "商品名：" + itemName + "\n価格：" + itemPrice + "円\n" + log;
            //画面表示（TextViewを使用）
            textView.setText(text);

        }
    }
}