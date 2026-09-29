# Tieba Lite 模块列表

<table>
  <tr>
   <td><strong>Name</strong>
   </td>
   <td><strong>Responsibilities</strong>
   </td>
  </tr>
  <tr>
   <td><code>app</code>
   </td>
   <td>Brings everything together required for the app to function correctly. This includes UI scaffolding and navigation. 
   </td>
  </tr>
  <tr>
   <td>core:common
   </td>
   <td>Common classes shared between modules.
   </td>
  </tr>
  <tr>
   <td><code>core:data</code>
   </td>
   <td>Fetching app data from multiple sources, shared by different features. <code>当前迁移进度: 2%</code>
   </td>
  </tr>
  <tr>
   <td>core:database
   </td>
   <td>Local database storage using Room.
   </td>
  </tr>
  <tr>
   <td><code>core:designsystem</code>
   </td>
   <td>Design system which includes Core UI components (many of which are customized Material 3 components), app theme and icons.
   </td>
  </tr>
  <tr>
   <td>core:network
   </td>
   <td>Making network requests and handling responses from a remote data source.
   </td>
  </tr>
  <tr>
   <td><code>core:ui</code>
   </td>
   <td>Composite UI components and resources used by feature modules. Unlike the <code>designsystem</code> module, it is dependent on the data layer since it renders models.
    <code>当前迁移进度: ?</code>
  </tr>
  <tr>
   <td><code>...</code>
   </td>
   <td>缓慢迁移中...
   </td>
  </tr>
</table>
