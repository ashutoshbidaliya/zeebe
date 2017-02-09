import {createEventsBus} from './events';
import {$document} from './dom';

export function jsx(element, attributes, ...children) {
  if (typeof element === 'function') {
    return handleComponent(element, attributes, children);
  }

  return handleHtml(element, attributes, children);
}

function handleComponent(component, attributes, children) {
  return component({
    children,
    ...attributes
  });
}

function handleHtml(element, attributes, children) {
  const template = (node, eventsBus) => {
    const elementNode = $document.createElement(element);

    node.appendChild(elementNode);

    if (attributes) {
      setAttributes(elementNode, attributes);
    }

    return addChildren(elementNode, eventsBus, children);
  };

  //For debuging only, so disabled in production
  if (process.env.NODE_ENV !== 'production') {
    template.element = element;
    template.attributes = attributes;
    template.children = children;
  }

  return template;
}

function setAttributes(elementNode, attributes) {
  Object
    .keys(attributes)
    .forEach((attribute) => {
      const value = attributes[attribute];

      if (isValidAttributeValue(value)) {
        setAttribute(elementNode, attribute, value);
      }
    });
}

function isValidAttributeValue(value) {
  return typeof value === 'string' || typeof value === 'number';
}

function setAttribute(elementNode, attribute, value) {
  if (attribute === 'className') {
    return elementNode.setAttribute('class', value);
  }

  elementNode.setAttribute(attribute, value);
}

export function addChildren(elementNode, eventsBus, children, shouldAddEventsBus) {
  return children.reduce((updates, child) => {
    return updates.concat(addChild(elementNode, eventsBus, child, shouldAddEventsBus));
  }, []);
}

export function addChild(elementNode, eventsBus, child, shouldAddEventsBus) {
  if (typeof child === 'string') {
    elementNode.appendChild(
      $document.createTextNode(child)
    );

    return [];
  }

  const childEventBus = createEventsBus(eventsBus);
  const update = child(elementNode, childEventBus);

  if (shouldAddEventsBus) {
    return {
      update,
      eventsBus: childEventBus
    };
  }

  return update;
}
