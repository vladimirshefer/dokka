import React, { useCallback, useState } from 'react';
import {Select} from '@jetbrains/ring-ui';
import { DokkaFuzzyFilterComponent } from '../search/dokkaFuzzyFilter';
import { IWindow, Option } from '../search/types';
import './navigationPaneSearch.scss';

type NavigationPaneProps = {
    data: Option[] 
}

export const NavigationPaneSearch = (props: NavigationPaneProps) => {
    const [selected, onSelected] = useState<Option | null>(null);
    const onChangeSelected = useCallback(
        (element: Option) => {
            window.location.replace(`${(window as IWindow).pathToRoot}${element.location}`)
            onSelected(element);
        },
        [props]
    );
    //@ts-ignore
    return <DokkaFuzzyFilterComponent
                id="navigation-pane-search"
                className="navigation-pane-search"
                inputPlaceholder="Title filter"
                filter={{fuzzy:true}}
                type={Select.Type.INPUT_WITHOUT_CONTROLS}
                clear
                selected={selected}
                data={props.data}
                popupClassName={"navigation-pane-popup"}
                onSelect={onChangeSelected}
            />
}