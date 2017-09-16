package com.example.administrator.healthmanager2.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.example.administrator.healthmanager2.R;
import com.example.administrator.healthmanager2.activity.MapActivity;
import com.example.administrator.healthmanager2.activity.preActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.example.administrator.healthmanager2.R.mipmap.getout;


/**
 * Created by Jay on 2015/8/28 0028.
 */
public class MyFragment4 extends Fragment {

    private static Map<String, String> additionalSummaryTexts;
    public MyFragment4()

    {

    }




    private ListView list;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fg_content,container,false);
        list = (ListView) view.findViewById(R.id.list);
        TextView txt_content = (TextView) view.findViewById(R.id.textView);
        ImageView img = (ImageView) view.findViewById(R.id.IV);
        img.setImageResource(getout);
        txt_content.setText("退出");

        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("健康运动管家");
                builder.setMessage("确认退出？");
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                         getActivity().finish();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();


               /* System.exit(0);*/
            }
        });

        init();

        return view;
    }



    private void init() {

        //生成动态数组，并且转载数据
        final ArrayList<HashMap<String, String>> mylist = new ArrayList<HashMap<String, String>>();

        HashMap<String, String> map = new HashMap<String, String>();


        map = new HashMap<String, String>();
        map.put("ItemTitle", "个人设置");
        map.put("ItemText", "");
        mylist.add(map);
        map = new HashMap<String, String>();
        map.put("ItemTitle", "百度地图");
        map.put("ItemText", "");
        mylist.add(map);

        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position,
                                    long id) {
                // TODO Auto-generated method stub
                if(position==0)
                {
                  /*  Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);*/
                    Intent intent = new Intent(getActivity(), preActivity.class);
                    startActivity(intent);
                }
                if(position==1)
                {
                  /*  Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);*/
                    Intent intent = new Intent(getActivity(), MapActivity.class);
                    startActivity(intent);
                }

            }
        });

        SimpleAdapter adapter = new SimpleAdapter(this.getActivity(), //没什么解释
                mylist,//数据来源
                R.layout.listview,//ListItem的XML实现
                //动态数组与ListItem对应的子项
                new String[] {"ItemTitle", "ItemText"},
                //ListItem的XML文件里面的两个TextView ID
                new int[] {R.id.ItemTitle,R.id.ItemText});
        list.setAdapter(adapter);
    }

}




