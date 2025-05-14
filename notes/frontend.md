# 前端

[toc]

---

## 创建基于ant-design-pro的React项目



我们通过[这个网站](https://pro.ant.design/zh-CN/docs/getting-started/)可以查看基本的开始教程

### 初始化

```shell
# 使用 npm
npm i @ant-design/pro-cli -g
pro create mbi-frontend
```



我们有四种包管理工具可以选择: `npm` `yarn` `cnpm` `tyarn`

这里我们选择`yarn`

>注意, 如果你选择yarn, 就要删除所有其它包管理工具的使用痕迹, 否则安装源配置会出错

我们可以使用`npm`来安装`yarn`

```shell
npm i yarn -g
```

随后切换`yarn`的镜像源

```shell
yarn install --registry=https://registry.npmmirror.com #淘宝镜像
```

然后就是安装包了

```shell
yarn install 
```

###  常见问题

####  mock模拟数据失效

如果你想要启用`mock`功能, 最好使用指令

```shell
yarn run start
```

否则会出现`Request 404`

#### 菜单消失

你会发现虽然路由存在，但是菜单找不到了, 这是因为你的`router.ts`的`path`没有添加`name`

导致的, 添加上`name`熟悉即可

```typescript
export default [
  { path: '/user', name:"用户", layout: false, routes: [{ path: '/user/login', component: './User/Login' }] },
  { path: '/welcome', name:"欢迎", icon: 'smile', component: './Welcome' },
  {
    path: '/admin',
    name:"管理员",
    icon: 'crown',
    access: 'canAdmin',
    routes: [
      { path: '/admin', name:"管理员页面", redirect: '/admin/sub-page' },
      { path: '/admin/sub-page', name:"二级管理页", component: './Admin' },
    ],
  },
  { icon: 'table', path: '/list', component: './TableList' },
  { path: '/', redirect: '/welcome' },
  { path: '*', layout: false, component: './404' },
];

```

####  提示模块‘umi‘没有导出的成员XXX

`umi` 项目安装依赖后，某个tsx文件中的代码：

```typescript
import { XXX } from 'umi';
```



有报错提示：`模块'umi'没有导出的成员XXX`。

原因：`ts` 对` umi` 的识别。

解决方式：

    首先，查看下 tsconfig.json 文件的配置是否正确。
    
    "paths": {
      "@/*": ["./src/*"],
      "@@/*": ["./src/.umi/*"]
    }
    
    如果上述配置正确，还是报错，可以采用以下两种方式解决：

1）关闭`vscode`，重新打开项目查看报错消失。

2）`command + p`，输入：`restart TS Server`，重启 `TS` 服务。



#### 使用`cookie`

我们可以在openapi 配置里面设置

```typescript
/**
 * @name request 配置，可以配置错误处理
 * 它基于 axios 和 ahooks 的 useRequest 提供了一套统一的网络请求和错误处理方案。
 * @doc https://umijs.org/docs/max/request#配置
 */
export const request = {
  baseURL: "http://localhost:8301/",
  withCredentials: true,
  ...requestConfig,
};
```

如果要携带`jwt`, 可以在`./requestConfig.ts`下使用全局拦截器

```typescript

  // 请求拦截器
  requestInterceptors: [
    (config: RequestOptions) => {
      // 拦截请求配置，进行个性化处理。
      const url = config?.url?.concat('?token = 123');
      return { ...config, url };
    },
  ],

  // 响应拦截器
  responseInterceptors: [
    (response) => {
      // 拦截响应数据，进行个性化处理
      const { data } = response as unknown as ResponseStructure;

      if (data?.success === false) {
        message.error('请求失败！');
      }
      return response;
    },
  ],
```



### 前后端联调

我们使用自带的openapi工具进行联调

首先在`config/config.ts`中找到`OpenAPI配置`

修改配置

```typescript
  openAPI: [
    {
      requestLibPath: "import { request } from '@umijs/max'",
      schemaPath: "http://localhost:8301/api/v2/api-docs",
      projectName: 'mbi-backend',
      mock: false,
    }
```

随后使用yarn调用

```shell
yarn run openapi
```

随后在`app.tsx`中修改请求地址

```typescript
/**
 * @name request 配置，可以配置错误处理
 * 它基于 axios 和 ahooks 的 useRequest 提供了一套统一的网络请求和错误处理方案。
 * @doc https://umijs.org/docs/max/request#配置
 */
export const request = {
  baseURL: "http://localhost:8301/",
  ...errorConfig,
};

```

### 改造初始项目以及相关模板文件介绍

部分示例文件其实是不需要的，我们删掉就好

1.  `mock`文件
2.  `types`文件

3.  `prettier`, `eslint`, `editorConfig`, `stylelint `是用来保证前端项目的代码规范

4.  `jest.config.ts`前端UI测试, 可删除
5.   由于已经移除了国际化组件, 所以可以直接删除`locale`文件

6.  替换标题 : 全局替换`Ant Design Pro`和 `Ant Design`