package gps.aldaleel.gps;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import gps.aldaleel.gps.BuildConfig;
import gps.aldaleel.gps.R;

public class dbDataActivity extends AppCompatActivity {

    @BindView(R.id.txt_result2)
    TextView txtresult;

    String result = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_data);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume(){
        super.onResume();
        onresukltClick();
    }


    public void onresukltClick(){
        try{
            SQLiteDatabase mydatabase = openOrCreateDatabase("aldaleel",MODE_PRIVATE,null);
            final String sqlGet = "SELECT * FROM `Schools`";
            Cursor c = mydatabase.rawQuery(sqlGet,null);
            if (c.moveToFirst()){
                do {
                    String column1 = c.getString(0);
                    String column2 = c.getString(1);
                    String column3 = c.getString(2);
                    String schoolName = c.getString(3);
                    result+="الإسم : "+schoolName+", إسم البلدية : "+column1+"\n";
                    Toast.makeText(getApplicationContext(),"before",Toast.LENGTH_LONG).show();
                } while(c.moveToNext());
            }
            if(txtresult!=null){
                txtresult.setText(result);
            }
            Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
            c.close();
            mydatabase.close();
        }
        catch(Exception exp){
            Log.e("stuff:",exp.getMessage());
        }
    }
}
