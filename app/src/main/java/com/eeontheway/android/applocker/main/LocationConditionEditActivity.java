package com.eeontheway.android.applocker.main;

import android.app.Fragment;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.locate.LocationService;
import com.eeontheway.android.applocker.locate.Position;
import com.eeontheway.android.applocker.lock.GpsLockCondition;

import java.util.ArrayList;
import java.util.List;

/**
 * 百度地图的位置选择点
 * 用于在地图上选择一个位置，然后将位置坐标保存起来，用于设置位置锁
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class LocationConditionEditActivity extends AppCompatActivity {
    private static final String PARAM_EDIT_MODE = "add_mode";
    private static final String PARAM_POS_CONFIG = "position";

    private boolean marked;
    private GpsLockCondition gpsLockCondition;
    private Position position;

    private MapView mapView;
    private RadioGroup rg_map_type;
    private View bt_show_my_location;
    private AutoCompleteTextView act_search;
    private Button bt_sumbit;
    private ArrayAdapter<String> act_adapter;

    private BaiduMap baiduMap;
    private LocationService locationService;
    private GeoCoder geocoderSearch;
    private PoiSearch mPoiSearch;
    private SuggestionSearch mSuggestionSearch;
    private BitmapDescriptor bdA;

    private boolean editMode;

    /**
     * 以编辑模式启动Activity
     * @param fragment
     * @param condition 编辑条件
     * @param requestCode 请求码
     */
    public static void start (Fragment fragment, GpsLockCondition condition, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), LocationConditionEditActivity.class);
        intent.putExtra(PARAM_EDIT_MODE, true);
        intent.putExtra(PARAM_POS_CONFIG, condition);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 启动界面，用于创建新项
     * @param fragment 上下文
     */
    public static void start (Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), LocationConditionEditActivity.class);
        intent.putExtra(PARAM_EDIT_MODE, false);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 获取结果位置条件
     * @param intent
     * @return 时间条件
     */
    public static GpsLockCondition getCondition (Intent intent) {
        return (GpsLockCondition)intent.getSerializableExtra(PARAM_POS_CONFIG);
    }

    /**
     * 判断是否是编辑模式
     * @param intent
     * @return true/false
     */
    public static boolean isEditMode (Intent intent) {
        return intent.getBooleanExtra(PARAM_EDIT_MODE, false);
    }

    /**
     * Activity的onCreate回调
     * @param savedInstanceState 之前保存的数据
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_select);

        setTitle(R.string.select_time);

        initToolBar();
        initView ();
        initMap();
        initLocation();
        rg_map_type.check(R.id.rb_normal);
    }

    /**
     * Activity的onDestroy回调
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        mapView.onDestroy();
    }

    /**
     * Activity的onResume回调
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * Activity的onPause回调
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }


    /**
     * Activiy的onCreateOptionMenu回调
     *
     * @param menu 创建的菜单
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu_position_sumbit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 处理返回按钮按下的响应
     *
     * @param item 被按下的项
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.action_submit:
                if (marked) {
                    Intent intent = getIntent();
                    intent.putExtra(PARAM_POS_CONFIG, gpsLockCondition);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(LocationConditionEditActivity.this,
                            R.string.postion_not_marked, Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * 按返回键时的处理
     */
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
        super.onBackPressed();
    }

    /**
     * 初始化ToolBar
     */
    private void initToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.tl_header);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    /**
     * 初始化View
     */
    private void initView () {
        initSearchEdit();

        // 配置自我定位按钮
        bt_show_my_location = findViewById(R.id.bt_show_my_location);
        bt_show_my_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 显示我的位置
                Position myPosition = locationService.getLastPosition();
                if (myPosition != null) {
                    showLocation(myPosition);
                }
            }
        });

        // 获取地图对像
        mapView = (MapView) findViewById(R.id.map_baidu);

        // 初始化图层选择监听器，进入普通地图模式
        rg_map_type = (RadioGroup)findViewById(R.id.rg_map_type);
        rg_map_type.setOnCheckedChangeListener(new MapTypeChangeListener());
    }

    /**
     * 初始化搜索编辑框
     */
    private void initSearchEdit() {
        // 配置自动完成输入框
        act_search = (AutoCompleteTextView) findViewById(R.id.act_search);
        act_adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        act_search.setAdapter(act_adapter);
        act_search.setThreshold(1);
        act_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() <= 0) {
                    return;
                }

                // 请求查询，以获取查询列表
                SuggestionSearchOption option = new SuggestionSearchOption();
                option.city("广州");
                option.keyword(s.toString());
                mSuggestionSearch.requestSuggestion(option);
            }
        });

        // 获取搜索建议
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(new OnGetSuggestionResultListener() {
            @Override
            public void onGetSuggestionResult(SuggestionResult suggestionResult) {
                if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
                    return;
                }

                // 获取所有查询结果
                List<String> suggestList = new ArrayList<>();
                for (SuggestionResult.SuggestionInfo info : suggestionResult.getAllSuggestions()) {
                    if (info.key != null) {
                        suggestList.add(info.key);
                    }
                }

                Log.d("Count", "" + suggestList.size());

                // 添加到适配器中，通知数据发生变化，显示出来
                act_adapter.clear();
                act_adapter.notifyDataSetChanged();
                act_adapter.addAll(suggestList);
                act_adapter.notifyDataSetChanged();
                act_search.showDropDown();
            }
        });

        // 配置搜索按钮
        bt_sumbit = (Button) findViewById(R.id.bt_sumbit);
        bt_sumbit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyWords = act_search.getText().toString();
                if (keyWords.isEmpty()) {
                    Toast.makeText(LocationConditionEditActivity.this,
                                            R.string.input_empty, Toast.LENGTH_SHORT).show();
                } else {
                    // 开始搜索
                    PoiCitySearchOption option = new PoiCitySearchOption();
                    option.city("广州");
                    option.keyword(act_search.getText().toString());
                    mPoiSearch.searchInCity(option);
                }
            }
        });
    }

    /**
     * 初始化地图
     */
    private void initMap () {
        bdA = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);

        // 初始化地图对像
        baiduMap = mapView.getMap();
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // 清除之前的所有选中点
                baiduMap.clear();

                // 添加标记
                MarkerOptions option = new MarkerOptions().title("hello").icon(bdA).position(latLng);
                baiduMap.addOverlay(option);

                // 同时保存选中的位置, 保存当前位置
                position.setLatitude(latLng.latitude);
                position.setLongitude(latLng.longitude);

                // 搜索找到其地址
                geocoderSearch.reverseGeoCode(new ReverseGeoCodeOption().location(latLng));

                // 标记为已经选择地址
                marked = true;
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                return false;
            }
        });

        // 初始化PIO Search
        initPIOSearch();

        // 初始化反向地址编码
        geocoderSearch = GeoCoder.newInstance();
        geocoderSearch.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                if ((reverseGeoCodeResult == null) ||
                        (reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR)) {
                    Toast.makeText(LocationConditionEditActivity.this,
                                        R.string.unknwon_location,Toast.LENGTH_LONG).show();
                    return;
                }

                // 保存当前地址
                position.setAddress(reverseGeoCodeResult.getAddress());
            }
        });
    }

    /**
     * 初始化PIO搜索
     */
    private void initPIOSearch() {
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult poiResult) {
                if ((poiResult == null) ||
                        (poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND)) {
                    Toast.makeText(LocationConditionEditActivity.this,
                                        R.string.no_result, Toast.LENGTH_LONG).show();
                    return;
                } else if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
                    baiduMap.clear();

                    // 添加标记，显示在地图上
                    PoiOverlay overlay = new MyPoiOverlay(baiduMap);
                    baiduMap.setOnMarkerClickListener(overlay);
                    overlay.setData(poiResult);
                    overlay.addToMap();
                    overlay.zoomToSpan();
                }
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
                if (poiDetailResult.error != SearchResult.ERRORNO.NO_ERROR) {
                    Toast.makeText(LocationConditionEditActivity.this,
                                R.string.no_result, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LocationConditionEditActivity.this,
                            poiDetailResult.getName() + ": " + poiDetailResult.getAddress(),
                            Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    /**
     * 开始定位模式
     * 地图模式定位在当前位置
     */
    private void initLocation () {
        // 配置定位监听
        baiduMap.setMyLocationEnabled(true);
        locationService = ((StartupApplcation)getApplication()).locationService;
        locationService.setPositionListener(new LocationService.PositionChangeListener() {
            @Override
            public void onPositionChanged(Position oldPosition, Position newPosition) {
                updateMyLocation(newPosition);
            }
        });

        // 读取配置参数
        Intent intent = getIntent();
        editMode = intent.getBooleanExtra(PARAM_EDIT_MODE, false);
        if (editMode) {
            gpsLockCondition = (GpsLockCondition) intent.getSerializableExtra(PARAM_POS_CONFIG);
            position = gpsLockCondition.getPosition();

            // 添加标记
            LatLng latLng = new LatLng(position.getLatitude(), position.getLongitude());
            MarkerOptions option = new MarkerOptions().title("hello").icon(bdA).position(latLng);
            baiduMap.addOverlay(option);
        } else {
            // 创建一个缺省对像,使能
            gpsLockCondition = new GpsLockCondition();
            position = locationService.getLastPosition().clone();
            gpsLockCondition.setPosition(position);
            gpsLockCondition.setEnable(true);
        }

        // 刷新下自己的位置，显示定位
        updateMyLocation(locationService.getLastPosition());
        showLocation(position);
    }

    /**
     * 在地图上更新我的当前位置
     * @param position 当前位置
     */
    private void updateMyLocation (Position position) {
        // 在地图上显示我的位置
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(position.getRadius())
                .direction(100).latitude(position.getLatitude())
                .longitude(position.getLongitude()).build();
        baiduMap.setMyLocationData(locData);
    }

    /**
     * 显示指定位置
     * @param position 指定位置
     */
    private void showLocation (Position position) {
        LatLng ll = new LatLng(position.getLatitude(), position.getLongitude());
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(ll).zoom(18.0f);
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    /**
     * 图层选择监听器
     */
    private class MapTypeChangeListener implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.rb_normal:        // 普通图层
                    baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                    break;
                case R.id.rb_satellite:     // 卫星图层
                    baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                    break;
            }
        }
    }

    /**
     * Marker图层
     */
    private class MyPoiOverlay extends PoiOverlay {
        public MyPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPoiClick(int index) {
            super.onPoiClick(index);
            PoiInfo poi = getPoiResult().getAllPoi().get(index);
            mPoiSearch.searchPoiDetail((new PoiDetailSearchOption()).poiUid(poi.uid));
            return true;
        }
    }
}
