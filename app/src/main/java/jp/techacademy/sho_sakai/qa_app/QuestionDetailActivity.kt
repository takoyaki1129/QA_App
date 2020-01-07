package jp.techacademy.sho_sakai.qa_app

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ListView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.list_question_detail.*

import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {


    //【追記】Favorite用の値定義
    private var mFavorite = 0
    //【追記】Favorite用の値定義
    private var mFavoriteRef: DatabaseReference? = null

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mListView: ListView
    private lateinit var mQuestionArrayList: ArrayList<Question>


    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
                // TODO:
            }
        }

        //【追記】お気に入りボタンを取得
        val buttonFavorite = findViewById<Button>(R.id.buttonFavorite)
        //【追記】ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        //【追記】ログインしていなければボタンをグレーアウトする
        if (user == null) {
            buttonFavorite.isEnabled = false
            buttonFavorite.text = "ログインしてください"
        }
        //【追記】ログインしている場合、ボタン押下でお気に入りに登録する
        else {
            if (mFavorite != 1) {
                buttonFavorite.text = "お気に入りに登録する"
                buttonFavorite.setOnClickListener {
                    mFavorite = 1
                }
            } else {
                buttonFavorite.text = "お気に入りから削除する"
                buttonFavorite.setOnClickListener {
                    mFavorite = 0
                }
            }

            //【追記】お気に入りor notを質問一覧画面に渡す
            val intent = Intent(applicationContext, QuestionsListAdapter::class.java)
            intent.putExtra("favorite", mFavorite)

            //【追記】質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す←MainActivity表示時に更新されるから要らない？
            //mQuestionArrayList.clear()
            //mAdapter.setQuestionArrayList(mQuestionArrayList)
            //mListView.adapter = mAdapter

            //【追記】選択したジャンルにリスナーを登録する
            if (mFavoriteRef != null) {
                mFavoriteRef!!.removeEventListener(mEventListener)
            }
            mFavoriteRef = mDatabaseReference.child(ContentsPATH).child(mFavorite.toString())
            mFavoriteRef!!.addChildEventListener(mEventListener)

            //【予備】各質問へのfavoriteの設定方法試行錯誤中/intent.putExtra("favorite", mFavorite)
            // val intent = Intent(applicationContext, QuestionSendActivity::class.java)
            //startActivity(intent)

            //【追記】この質問に対するユーザーのお気に入り値を保存する
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = sp.edit()
            editor.putInt(FavoriteKEY, mQuestion.favorite)
            editor.commit()

        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)
    }
}
