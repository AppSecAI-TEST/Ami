package chao.app.ami.launcher.drawer;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

import chao.app.ami.ActivitiesLifeCycleAdapter;
import chao.app.ami.Ami;
import chao.app.ami.UI;
import chao.app.ami.classes.ClassesManager;
import chao.app.ami.classes.Frame;
import chao.app.ami.classes.FrameAdapter;
import chao.app.ami.hooks.FragmentLifecycle;
import chao.app.ami.viewinfo.InterceptorLayerManager;
import chao.app.debug.R;


public class DrawerManager implements DrawerXmlParser.DrawerXmlParserListener, View.OnClickListener, WindowCallbackHook.DispatchKeyEventListener, ClassesManager.TopFrameChangedListener, ViewGroup.OnHierarchyChangeListener {

    private static DrawerManager sDrawerManager;

    private RecyclerView mDrawerListView;
    private DrawerAdapter mDrawerAdapter;

    private RecyclerView mFrameListView;
    private FrameAdapter mFrameAdapter;
    private ImageView mFrameNavigationBackView;
    private TextView mFrameNavigationPathView;

    private DrawerNode mDrawerRootNode;

    private DrawerLayout mDrawerLayout;

    private ImageView mNavigationBackView;
    private TextView mNavigationPathView;

    private int mDrawerId;

    private InterceptorLayerManager mInterceptorManager;
    private ViewGroup mRealView;
    private ViewGroup mDecorView;

    private WeakReference<Context> mContext;

    private Application mApp;

    DrawerManager(Application app) {
        mApp = app;
    }


    private void setupView(Activity activity) {
        FrameLayout decorView = (FrameLayout) activity.findViewById(android.R.id.content);
        int decorViewChildCount = decorView.getChildCount();
        if (decorViewChildCount == 0) {
            return;
        }
        if (mDecorView == decorView) {
            return;
        }
        FrameLayout realView = new FrameLayout(activity);
        realView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View[] decorChildren = new View[decorViewChildCount];
        for (int i=0; i<decorViewChildCount; i++) {
            decorChildren[i] = decorView.getChildAt(i);
        }
        decorView.removeAllViews();
        for (int i=0;i<decorViewChildCount;i++) {
            View child = decorChildren[i];
            if (child == null) {
                continue;
            }
            realView.addView(child);
        }

        if (mDecorView != null) {
            mInterceptorManager.cleanSelected();
            mDrawerLayout.removeView(mRealView);
            mDecorView.removeAllViews();
            if (mRealView != null) {
                mDecorView.addView(mRealView);
            }
        }

        mDecorView = decorView;
        mRealView = realView;
        mContext = new WeakReference<>(mDecorView.getContext());

        if (mDrawerLayout == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext.get());
            mDrawerLayout = (DrawerLayout) inflater.inflate(R.layout.drawer_launcher, mDecorView, false);
            FrameLayout content = (FrameLayout) mDrawerLayout.findViewById(R.id.ami_content);

            mInterceptorManager = InterceptorLayerManager.get();
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            content.addView(mInterceptorManager.getLayout(),layoutParams);

            View componentContent = findViewById(R.id.drawer_component_content);
            mNavigationBackView = (ImageView) componentContent.findViewById(R.id.navigation_back);
            mNavigationBackView.setOnClickListener(this);
            mNavigationPathView = (TextView) componentContent.findViewById(R.id.navigation_title);
            mDrawerListView = (RecyclerView) componentContent.findViewById(R.id.ui_list);
            mDrawerListView.setLayoutManager(new LinearLayoutManager(mContext.get(), LinearLayoutManager.VERTICAL, false));
            mDrawerListView.addItemDecoration(new DividerItemDecoration(mContext.get(), LinearLayoutManager.VERTICAL));


            ClassesManager classesManager = ClassesManager.getInstance();
            classesManager.addFrameChangeListener(this);
            View frameContent = findViewById(R.id.drawer_frame_content);
            mFrameListView = (RecyclerView) frameContent.findViewById(R.id.frame_list);
            mFrameListView.setLayoutManager(new LinearLayoutManager(mContext.get(), LinearLayoutManager.VERTICAL, false));
            mFrameListView.addItemDecoration(new DividerItemDecoration(mContext.get(), LinearLayoutManager.VERTICAL));
            mFrameAdapter = new FrameAdapter();
            mFrameListView.setAdapter(mFrameAdapter);
            mFrameNavigationBackView = (ImageView) frameContent.findViewById(R.id.navigation_back);
            mFrameNavigationBackView.setOnClickListener(this);
            mFrameNavigationPathView = (TextView) frameContent.findViewById(R.id.navigation_title);

            mDrawerLayout.addDrawerListener(mFrameAdapter);

//            ViewInterceptor interceptor = new ViewInterceptor();
//            mInterceptorFrame.setInterceptor(interceptor);

            DrawerXmlParser parser = new DrawerXmlParser();
            parser.parseDrawer(mContext.get().getResources().openRawResource(mDrawerId), this);
        }
        mInterceptorManager.injectListeners(mRealView);
        mDecorView.addView(mDrawerLayout);
        mDrawerLayout.addView(mRealView, 0);
    }

    private <T extends View> T findViewById(int resId) {
        return (T) mDrawerLayout.findViewById(resId);
    }

    private void setDrawerId(int rawId) {
        mDrawerId = rawId;
    }

    @Override
    public void onXmlParserDone(DrawerNode rootNode) {
        mDrawerRootNode = rootNode;
        mDrawerAdapter = new DrawerAdapter(mDrawerRootNode);
        mDrawerListView.setAdapter(mDrawerAdapter);
    }

    @Override
    public void onXmlParserFailed(Exception e) {

    }

    @Override
    public void onClick(View v) {
        if (v == mNavigationBackView) {
            mDrawerAdapter.navigationUp();
        } else if (v == mFrameNavigationBackView) {
            mFrameAdapter.navigationUp();
        }
    }

    @Override
    public boolean onDispatchKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawers();
                return true;
            }
        }
        return false;
    }

    public void notifyFrameChanged() {
        if (mFrameAdapter != null) {
            mFrameAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onTopFrameChanged(Frame frame, String path) {
        mFrameNavigationPathView.setText(path);
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        mInterceptorManager.injectListeners(child);
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        //do nothing
    }

    private class DrawerAdapter extends RecyclerView.Adapter {

        private NodeGroup mCurrentGroup;

        private DrawerAdapter(NodeGroup groupNode) {
            mCurrentGroup = groupNode;
            updateNavigation();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(mContext.get()).inflate(R.layout.simpel_drawer_item_layout, parent, false)) {};
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            View itemView = holder.itemView;
            TextView textView = (TextView) itemView.findViewById(R.id.drawer_item_name);
            ImageView arrow = (ImageView) itemView.findViewById(R.id.drawer_item_arrow);
            Node node = mCurrentGroup.getChild(position);
            int visible = View.INVISIBLE;
            if (node instanceof NodeGroup) {
                visible = View.VISIBLE;
            }
            arrow.setVisibility(visible);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Node node = mCurrentGroup.getChild(holder.getAdapterPosition());
                    if (node instanceof NodeGroup) {
                        NodeGroup group = (NodeGroup) node;
                        mDrawerAdapter.navigationTo(group);
                    } else if (node instanceof ComponentNode){
                        ComponentNode componentNode = (ComponentNode) node;
                        try {
                            mDrawerLayout.closeDrawer(GravityCompat.START, false);
                            Class<?> clazz = componentName(componentNode);
                            UI.show(mContext.get(), clazz, componentNode.getBundle(), componentNode.getFlags());
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            textView.setText(mCurrentGroup.getChild(position).getName());
        }

        private Class componentName(ComponentNode node) throws ClassNotFoundException {
            String packageName = mDrawerRootNode.getPackageName();
            String component = node.getComponent();
            if (component.charAt(0) == '.') {
                component = packageName + component;
            }
            return Class.forName(component);
        }

        @Override
        public int getItemCount() {
            return mCurrentGroup.size();
        }

        public void navigationUp() {
            NodeGroup group = mCurrentGroup.getParent();
            if (group != null) {
                mCurrentGroup = group;
                notifyDataSetChanged();
                updateNavigation();
            }
        }

        public void navigationTo(NodeGroup groupNode) {
            if (groupNode == null) {
                return;
            }
            mCurrentGroup = groupNode;
            notifyDataSetChanged();
            updateNavigation();
        }

        private void updateNavigation() {
            if (mCurrentGroup.getParent() != null) {
                mNavigationBackView.setImageResource(R.drawable.ic_navigation);
            } else {
                mNavigationBackView.setImageResource(0);
            }
            mNavigationPathView.setText(buildNavigationTitle(mCurrentGroup));
        }

        private String buildNavigationTitle(Node node) {
            if (node == null) {
                return "/";
            }
            String title = node.getName();
            if (title == null) {
                title = "";
            }
            NodeGroup parent = node.getParent();
            while (parent != null) {
                String parentName = parent.getName();
                if (!TextUtils.isEmpty(parentName)) {
                    title = parent.getName() + "/" +  title;
                }
                parent = parent.getParent();
            }

            return title;
        }

    }

    public static void init(Application app, int drawerXml) {
        sDrawerManager = new DrawerManager(app);
        sDrawerManager.setDrawerId(drawerXml);
        app.registerActivityLifecycleCallbacks(new ActivitiesLifeCycleAdapter(){

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                //hook点击事件
                activity.getWindow().setCallback(WindowCallbackHook.newInstance(activity, sDrawerManager));

                //hook fragment
//                FragmentManagerHook.hook(activity);


                if (activity instanceof FragmentActivity) {
                    FragmentActivity fActivity = (FragmentActivity) activity;
                    FragmentManager supportManager = fActivity.getSupportFragmentManager();
                    supportManager.registerFragmentLifecycleCallbacks(new FragmentLifecycle(), true);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    Hooker.hookFragmentManager(activity.getFragmentManager());
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                sDrawerManager.setupView(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                sDrawerManager.injectInput(activity);
            }

            @Override
            public void onActivityStopped(Activity activity) {
                super.onActivityStopped(activity);
                sDrawerManager.mInterceptorManager.cleanSelected();
            }
        });
    }

    public void injectInput(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }
        Map<String, String> inputs = (Map<String, String>) intent.getSerializableExtra(DrawerConstants.EXTRA_KEY_INPUT);
        if (inputs == null) {
            return;
        }
        Set<String> viewIds = inputs.keySet();
        if (viewIds.size() == 0) {
            return;
        }
        for (String viewId : viewIds) {
            View view = null;
            String[] ids = viewId.split("\\.");
            if (ids.length == 0) {
                ids = new String[]{viewId};
            }
            for (String id: ids) {
                int resId = activity.getResources().getIdentifier(id, "id", Ami.getApp().getPackageName());
                if (view == null) {
                    view = activity.findViewById(resId);
                } else {
                    view = view.findViewById(resId);
                }
                if (view == null) {
                    return;
                }
                if (view instanceof TextView) {
                    ((TextView) view).setText(inputs.get(viewId));
                }
            }
        }
    }

    public static DrawerManager get() {
        if (sDrawerManager == null) {
            throw new NullPointerException("DrawerManager not initialization.");
        }
        return sDrawerManager;
    }

}