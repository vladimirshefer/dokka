import React from 'react';
import {render} from 'react-dom';
import RedBox from 'redbox-react';
import {Select, List} from '@jetbrains/ring-ui';

import App from "./app";
import './app/index.scss';
import { NavigationPaneSearch } from './navigationPaneSearch/navigationPaneSearch';

const appEl = document.getElementById('searchBar');
const rootEl = document.createElement('div');

let renderApp = () => {
  render(
      <App/>,
      rootEl
  );
};

// @ts-ignore
if (module.hot) {
  const renderAppHot = renderApp;
  const renderError = (error: Error) => {
    render(
        <RedBox error={error}/>,
        rootEl
    );
  };

  renderApp = () => {
    try {
      renderAppHot();
    } catch (error) {
      renderError(error);
    }
  };

  // @ts-ignore
  module.hot.accept('./app', () => {
    setTimeout(renderApp);
  });
}

renderApp();
appEl!.appendChild(rootEl);

const mock = Array.from(Array(10), (_, i) => {
  return {
    label: 'sample' + i,
    searchKey: 'sample'+ i,
    location: '',
    name: 'sample'+ i,
    kind: 'sample',
    description: '',
    disabled: false,
    key: i,
    rgItemType: List.ListProps.Type.CUSTOM
  }
})
render(
  <NavigationPaneSearch data={mock} />,
  document.getElementById('paneSearch')
)
