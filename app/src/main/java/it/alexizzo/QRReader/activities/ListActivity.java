package it.alexizzo.QRReader.activities;

import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import it.alexizzo.argonreader.R;

public class ListActivity extends AppCompatActivity {

    private static final String sTag = ListActivity.class.getSimpleName();


    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mRecyclerView=(RecyclerView) findViewById(R.id.rv_list);
        List<MyData> data = new ArrayList<>();
        data.add(new MyData("Alessandro", "Izzo", 27));
        data.add(new MyData("Mirko", "Rinaldini", 32));
        data.add(new MyData("Michele", "Izzo", 25));
        data.add(new MyData("Gonzalo", "Higuain", 29));
        data.add(new MyData("Antonio", "Izzo", 57));
        data.add(new MyData("Giusi", "Meli", 52));
        data.add(new MyData("Ciccio", "Merda", 25));
        data.add(new MyData("Salvatore", "Coco", 29));
        data.add(new MyData("Carmelo", "Calandra", 52));
        data.add(new MyData("Dario", "Prano", 25));
        data.add(new MyData("Cucù", "Settete", 29));
//        Log.d("ciao", data.get(0).toJson().toString());
        mAdapter = new MyAdapter();
        mAdapter.setData(data);
        //LinearLayoutManager makes your RecyclerView position its items exactly like ListView did
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration());
        mRecyclerView.setAdapter(mAdapter);
    }

    class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

        private List<MyData> mData;
        public MyAdapter(){}

        public void setData(List<MyData> data){
            mData=data;
        }

        public void addData(MyData data){
            if(mData==null) mData=new ArrayList<>();
            mData.add(data);
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(ListActivity.this)
                    .inflate(R.layout.list_view_item, viewGroup, false);
            MyViewHolder holder = new MyViewHolder(v, mData.get(i));
            return holder;
        }

//        This method internally calls onBindViewHolder(ViewHolder, int) to update the RecyclerView.ViewHolder contents
//        with the item at the given position and also sets up some private fields to be used by RecyclerView.
//        A view holder is an object attached to each row in your ListView
        @Override
        public void onBindViewHolder(MyViewHolder viewHolder, int i) {
            viewHolder.setData(mData.get(i));
        }

        @Override
        public int getItemCount() {
            return mData!=null?mData.size():0;
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder{

//        private View connectedView;
        private MyData data;

        public MyViewHolder(View itemView, MyData data) {
            super(itemView);
//            connectedView=itemView;
            this.data=data;
            populateView();
        }

        private void populateView(){
            if(data == null || itemView == null) return;

            ((TextView)itemView.findViewById(R.id.tv_name)).setText(data.getName());
            ((TextView)itemView.findViewById(R.id.tv_surname)).setText(data.getSurname());
            ((TextView)itemView.findViewById(R.id.tv_age)).setText(""+data.getAge());
        }

        public void setData(MyData data){
            this.data=data;
            populateView();
        }
    }

    class DividerItemDecoration extends RecyclerView.ItemDecoration {

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1) {
//                int left = view.getPaddingLeft();
//                int right = view.getWidth() - view.getPaddingRight();
//                Drawable divider= ContextCompat.getDrawable(MainActivity.this, R.drawable.divider);
//                divider.setBounds(left, 0, right, 0);
                outRect.bottom+=6;
            }
        }
    }

    class MyData {

        private String mName, mSurname;
        private int mAge;

        public MyData(){ this(null, null, -1); }
        public MyData(String name){ this(name, null, -1); }
        public MyData(String name, String surname){ this(name, surname, -1); }
        public MyData(String name, String surname, int age){
            mName=name;
            mSurname=surname;
            mAge=age;
        }

        public String getSurname() {
            return mSurname;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            this.mName = name;
        }

        public int getAge() {
            return mAge;
        }

        public void setAge(int age) {
            this.mAge = age;
        }

        public JSONObject toJson(){

            GsonBuilder builder = new GsonBuilder();
            builder.setFieldNamingStrategy(new FieldNamingStrategy() {
                @Override
                public String translateName(Field f) {
                    switch (f.getName()){
                        case "mName": return "name";
                        case "mSurname": return "surname";
                        case "mAge": return "age";
                        default: return null;
                    }
                }
            });

            Gson gson = builder.create();
            try {
                return new JSONObject(gson.toJson(this));
            } catch(JSONException e){
                e.printStackTrace();
            }
            return null;
        }
    }


}
