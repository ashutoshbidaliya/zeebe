import React from 'react';
import classnames from 'classnames';

import './ProgressBar.scss';

export default function ProgressBar({min, max, value, formatter}) {
  const relative = Math.min(1, Math.max(0, (value - min) / (max - min)));
  const goalPercentage = max * 100 / (value - min);
  const goalExceeded = max < value;

  return (
    <div className="ProgressBar">
      {goalExceeded && (
        <div
          className="goalOverlay"
          style={{
            width: `${goalPercentage}%`
          }}
        >
          <span className={classnames('goalLabel', {rightSide: goalPercentage > 50})}>
            Goal: {formatter(max)}
          </span>
        </div>
      )}
      <div className="ProgressBar__label">{formatter(value)}</div>
      <div className="ProgressBar--filled" style={{width: `${relative * 100}%`}} />
      <div
        className={classnames('ProgressBar__range', {
          'ProgressBar__range--belowBase': value <= min,
          'ProgressBar__range--aboveTarget': value >= max,
          flippedColors: goalExceeded
        })}
      >
        {formatter(min)}
        <span className="ProgressBar__range--right">
          {goalExceeded ? formatter(value) : 'Goal:' + max}
        </span>
      </div>
    </div>
  );
}
