3/16-20/2001: Hung-ying Tyan
Changes made to PtPlot5.1 (search for "DRCL")
- PlotBox:
  - open keylistener so that we can override key bindings and functions
  - add setLegend(...) to change legend
  - enables auto-ranging in fillPlot()
  - add another format button (and image) for formatting individual dataset,
    see DatasetFormatter also
  - add wrapY fields and methods
- Plot:
  - add stepwise setting and drawing
  - add getPoint(...) to get PlotPoint in a certain dataset
  - add getPoints(...) to get all PlotPoints of a certain dataset in PlotPoint[]
  - add getNumPoints() to get #points of a certain dataset
  - add isSetEmpty() to see if a dataset is empty
  - add getConnected(int dataset)
  - add getMarksStyle(int dataset)
  - add setWrapY(boolean)
  - separate drawing codes from _drawPlotPoint() to __drawPlotPoint() and make
    _drawPlot() use __drawPlotPoint(), optimized for both online drawing and EPS export
  - change the order of marks in _drawPoint()
- PlotFrame: create a new window to display sample plot.
- PlotFormatter: add "stepwise" drawing, add wrapY
- DatasetFormatter: new class, inspired by PlotFormatter, for formatting a dataset
- EPSGraphics: use color output instead of grayscale

TODO: 
- Plot: stepwise is not figured into saving in PlotML 
- PlotBox/Plot: setWrapY(...) does not consider connected attribute
- PlotBox/Plot: setWrap(...) should follow setWrapY(...)
- PlotBox/Plot: PlotML export do not save complete wrap information
- PlotBox: can be optimized for EPS export in _drawPlot(), but may slowdown online drawing...
