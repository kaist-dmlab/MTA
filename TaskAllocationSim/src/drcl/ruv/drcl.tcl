# drcl.tcl
# 
# File-system-like commands
# wildcard: ls, cp, mv, rm

package require java

# LIST OF COMMANDS (excluding file system commands)
# __common__
# __common_attach
# __common_cpmv
# __common_result_handling
# _getForkManager
# _getRuntime
# _getTerminal
# _is_mixed_path path_
# _parse
# _parse2
# _resolve_mixed_path mixedpath_
# _rt_common_
# _to_Paths
# _to_string_array list_
# attach
# attach_simulator
# autocomplete
# connect
# detach
# disconnect
# exit
# explore
# fm
# getflag
# grep
# inject
# jsim
# nlines
# pipe
# quit
# reset
# resume
# rt
# run
# script
# set_default_class args
# setflag
# stop
# subtext
# term
# verify
# wait_util
# watch
# whats_default_class

set _d_  "drcl.comp.Component"
set _gsys_ drcl.comp
set BOOLEAN_ARRAY "\[Z"
set DOUBLE_ARRAY "\[D"
set INT_ARRAY "\[I"
set LONG_ARRAY "\[J"
set FLOAT_ARRAY "\[F"
set BYTE_ARRAY "\[B"
set OBJECT_ARRAY_PREFIX "\[L"
set OBJECT_PREFIX "\[L"
set DEFAULT_CLASS "drcl.comp.Component"
set _ref_holder_ ""; # used to hold intermediate java object reference
set _system_monitor_ "/.system/monitor"
set _system_monitor_watch_port_ ".in"

# prevent re-entry the following block
if [catch {set _beenHere_drcl2_tcl_ $_beenHere_drcl2_tcl_}] then {
	set _pwd_ [java::field $_gsys_.Component Root]
	catch { rename cd 		__cd}
	catch { rename cat		__cat}
	catch { rename mkdir 	__mkdir}
	catch { rename pwd 		__pwd}
	catch { rename ls 		__ls}
	catch { rename cp 		__cp}
	catch { rename mv 		__mv}
	catch { rename ln 		__ln}
	catch { rename rm 		__rm}
	catch { rename term		__term}
	catch { rename exit		__exit}
	catch { rename quit		__quit}

	set _beenHere_drcl2_tcl_ 1
}

# general utility to parse flags or options
proc _parse {oldargs_} {
	set flags_ ""
	set args_ ""; # of invoked method

	set i 0
	foreach arg_ $oldargs_ {
		if {[string first "-" $arg_] == 0} {
			append flags_ [string range $arg_ 1 end]
		} else break;
		incr i
	}

	set a(flags) $flags_
	if {$i == [llength $oldargs_] - 1} {
		set a(args) [list [lindex $oldargs_ end]]
	} else {
		set a(args) [lrange $oldargs_ $i end]
	}

	return [array get a]
}

proc _parse2 {oldargs_ keywords_} {
	set flags_ ""
	set args_ ""; # of invoked method

	set i 0
	foreach arg_ $oldargs_ {
		if {[string first "-" $arg_] == 0 && [lsearch $keywords_ $arg_] < 0} {
			append flags_ [string range $arg_ 1 end]
		} else break;
		incr i
	}

	set a(flags) $flags_
	if {$i == [llength $oldargs_] - 1} {
		set a(args) [list [lindex $oldargs_ end]]
	} else {
		set a(args) [lrange $oldargs_ $i end]
	}

	return [array get a]
}

proc _to_string_array list_ {
	set array_ [java::new {String[]} [llength $list_]]
	set i 0
	foreach s $list_ {
		$array_ set $i [java::new String $s]
		incr i
	}
	return $array_
}

# convert java0x??/subpath to {java0x??, subpath}
proc _resolve_mixed_path mixedpath_ {
	# start with a Java reference
	if [string match "java0x*" $mixedpath_] {
		set index_ [string first "/" $mixedpath_]
		#puts "Java path: $mixedpath_"
		set i $index_
		incr index_ -1
		if {$index_ < 0} {return [list $mixedpath_ "."]}
		set pwd_ [string range $mixedpath_ 0 $index_]
		#puts "resolve_mixed_path, pwd = [$pwd_ toString]"
		for {incr i} {$i < [string length $mixedpath_]} {incr i} {
			if {[string index $mixedpath_ $i] == "/"} continue else break;
		}
		set path_ [string range $mixedpath_ $i end]
		if {$path_ == ""} { set path_ "."}
		#puts "resolve_mixed_path, Relative path: $path_"
		
		return [list $pwd_ $path_];
	} else {
		return [list [java::null] $path_];
	}
}

proc _is_mixed_path path_ {
	return [string match "java0x*" $path_];
}


# convert whatever paths to Java array of drcl.ruv.Paths's
# no error checking
proc _to_Paths {base_ paths_} {
	set len_ [llength $paths_]
	if {$len_ == 0} {return [java::new drcl.ruv.Paths\[\] 0]}
	set jpaths_ [java::new drcl.ruv.Paths\[\] $len_]
	set i 0
	foreach path_ $paths_ {
		#puts "convert '$path_' to Paths"
		if [_is_mixed_path $path_] {
			set tmp_ [_resolve_mixed_path $path_]
			set mbase_ [lindex $tmp_ 0]
			set spath_ [lindex $tmp_ 1]
			#puts "base $mbase_, path '$spath_'"
			if [java::instanceof $mbase_ drcl.comp.Component] {
				$jpaths_ set $i [java::new drcl.ruv.Paths $mbase_ $spath_]
			} else {
				#puts "noncomponent java reference"
				if [java::isnull $mbase_] {
					$jpaths_ set $i [java::new drcl.ruv.Paths [java::field drcl.ruv.Commands NULL_OBJECT]]
				} elseif [java::instanceof $mbase_ {Object[]}] {
					set allcomponents_ 1
					for {set j 0} {$j < [$mbase_ length]} {incr j} {
						set tmp_ [$mbase_ get $j]
						if [java::instanceof $tmp_ drcl.comp.Component] {
						} elseif [java::instanceof $tmp_ drcl.comp.Port] {
						} else {
							set allcomponents_ 0
							break
						}
					}
					if $allcomponents_ {
						#puts "array of components"
						$jpaths_ set $i [java::new drcl.ruv.Paths [!!! mbase_] $spath_]
					} else {
						#puts "noncomponent object"
						# treat it as noncomponent java reference
						$jpaths_ set $i [java::new drcl.ruv.Paths $mbase_]
					}
				} else {
					$jpaths_ set $i [java::new drcl.ruv.Paths $mbase_]
				}
			}
		} else {
			$jpaths_ set $i [java::new drcl.ruv.Paths $base_ [_to_string_array [list $path_]]]
		}
		incr i
	}
	return $jpaths_
}

proc man {{cmd_ ""}} {
	if {$cmd_ == ""} {
		puts [java::call drcl.ruv.Commands getAllCommands]
	} else {
		puts [java::call drcl.ruv.Commands man $cmd_]
	}
}

proc __common__ {cmd args_ {keywords_ ""}} {
	if {$keywords_ == ""} {
		array set argArray_ [_parse $args_]
	} else {
		array set argArray_ [_parse2 $args_ $keywords_]
	}
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)

	if {$args_ == ""} { set args_ "." }

	global _pwd_ __shell
	set jpaths_ [_to_Paths $_pwd_ $args_]
	return [java::call drcl.ruv.Commands $cmd $flags_ $jpaths_ $__shell]
}

proc __common_result_handling result_ {
	if [catch {java::isnull $result_}] {
		# not a java object
		return $result_
	}
	if [java::isnull $result_] { return $result_ }
	if {[java::instanceof $result_ Object\[\]] && [$result_ length] == 1} { return [!!! [$result_ get 0]] } 
	global _ref_holder_
	set _ref_holder_ $result_
	return [!!! result_]
}

proc __common_cpmv {op_ args_} {
	array set argArray_ [_parse $args_]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)

	global _pwd_ __shell
	set jpaths_ [_to_Paths $_pwd_ $args_]
	set result_ [java::call drcl.ruv.Commands "cpmv" $op_ $flags_ $jpaths_ $__shell]
	return [__common_result_handling $result_]
}

proc cp args { return [__common_cpmv "copy" $args] }
proc mv args { return [__common_cpmv "move" $args] }

proc ! args {
	array set argArray_ [_parse $args]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)
	if {[llength $args_] == 0} {return ""}
	set paths_ [lindex $args_ 0]
	if {[llength $args_] == 2} {
		set args_ [list [lindex $args_ end]]
	} else {
		set args_ [lrange $args_ 1 end]
	}

	#puts "paths: $paths_"
	#puts "args:  $args_"

	# get all the object references first
	global _pwd_ __shell
	set jpaths_ [_to_Paths $_pwd_ $paths_]
	set refs_ [java::call drcl.ruv.Commands "toRef" $flags_ $jpaths_ false $__shell]
	#puts $refs_

	# No arguments, just return the references
	# XXX: convert to list?
	if {$args_ == ""} { return [__common_result_handling $refs_] }

	set result_ ""
	set result2_ ""
	set count_ 0
	if [java::isnull $refs_] {
		return ""
	} else {
		set len_ [$refs_ length]
		for {set i 0} {$i < $len_} {incr i} {
			set obj_ [!!! [$refs_ get $i]]
			# XXX: handle exception
			set result_ [eval [subst -nocommands {$obj_ $args_}]]
			if {$result_!=""} {
				append result2_ "[$obj_ toString]: $result_\n"
			}
			#puts "$count_: $obj_/[$obj_ toString]"
			incr count_
		}
	}
	if {$count_ == 1} {
		if {$result_!=""} { return $result_ }
	} else {
		if {$result2_!=""} { return $result2_ }
	}
}

proc !! args {
	array set argArray_ [_parse $args]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)

	global _pwd_ __shell
	set jpaths_ [_to_Paths $_pwd_ $args_]
	set refs_ [java::call drcl.ruv.Commands "toRef" $flags_ $jpaths_ false $__shell]
	global _ref_holder_
	set _ref_holder_ $refs_
	return $refs_
}

# sorted version of "!!"
proc !!s args {
	array set argArray_ [_parse $args]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)

	global _pwd_ __shell
	set jpaths_ [_to_Paths $_pwd_ $args_]
	set refs_ [java::call drcl.ruv.Commands "toRef" $flags_ $jpaths_ true $__shell]
	global _ref_holder_
	set _ref_holder_ $refs_
	return $refs_
}

# cast the java object to the most specific class.
proc !!! javaobj {
	if [string match "java*" $javaobj] {
		# probably a java reference
		set directRef_ 1
		set ref $javaobj
	} else {
		set directRef_ 0
		upvar $javaobj obj
		set ref $obj
	}
	set ref [java::cast Object $ref]
	set class_ [$ref getClass]

	while 1 {
		set mod_ [$class_ getModifiers]
		if [java::call java.lang.reflect.Modifier isPublic $mod_] break
		set class_ [$class_ getSuperclass]
	}
	return [java::cast [$class_ getName] $ref]
}

proc cd args {
	global _pwd_
	set newd_ [__common__ "cd" $args]
	if [java::instanceof $newd_ drcl.comp.Component] { 
		set _pwd_ $newd_ 
	}
	puts -nonewline ""
}

proc rm args {
	set result_ [__common__ "rm" $args]
	return [__common_result_handling $result_]
}

proc cat args { return [__common__ "cat" $args] }

proc set_default_class args {
	global DEFAULT_CLASS
	if {$args == ""} {
		set DEFAULT_CLASS "drcl.comp.Component"
	} else {
		if [catch {java::new $args}] {
			puts "Invalid class name: '$args'"
			puts "Default class name remains: $DEFAULT_CLASS"
			return
		}
		set DEFAULT_CLASS $args
	}
	return
}

proc whats_default_class {} {
	global DEFAULT_CLASS
	return $DEFAULT_CLASS
}

proc mkdir args {
	global DEFAULT_CLASS
	array set argArray_ [_parse $args]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)
	if {[llength $args_] == 0} { return [java::null] }

	# suppose the first argument is a class name...
	set class_ [lindex $args_ 0]

	if [string match "*@*" $class_] {
		# turns out it's a port path
		set class_ "drcl.comp.Port"
	} elseif {[llength $args_] == 1} {
		set class_ $DEFAULT_CLASS
	} else {
		# treat it as a class name or a java object
		if {[llength $args_] == 2} {
			set args_ [list [lindex $args_ end]]
		} else {
			set args_ [lrange $args_ 1 end]
		}
	}

	global _pwd_ __shell
	set jpaths_ [_to_Paths $_pwd_ $args_]
	set result_ [java::call drcl.ruv.Commands mkdir $flags_ $class_ $jpaths_ false $__shell]
	set result_ [__common_result_handling $result_]
	# XXX: convert result to list?
	return $result_
}

proc pwd {} {
	global _pwd_ _gsys_
	puts [java::call $_gsys_.Util getFullID $_pwd_]
}

proc ls args { return [__common__ "ls" $args] }

proc __common_attach {cmd_ args_} {
	array set argArray_ [_parse $args_]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)

	if {$args_ == ""} { set args_ "." }

	global _pwd_ __shell
	set jpaths_ [_to_Paths $_pwd_ $args_]
	return [java::call drcl.ruv.Commands "attach" $cmd_ $flags_ $jpaths_ $__shell]
}

proc attach args { __common_attach "attach" $args }
proc detach args { __common_attach "detach" $args }
proc connect args { __common__ "connect" $args }
proc disconnect args { __common__ "disconnect" $args }
proc pipe args { __common__ "pipe" $args "-break -connect" }

# set component flags
proc setflag args {
	array set argArray_ [_parse $args]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)

	set i 0
	foreach arg_ $args_ {
		if {[string match "true" $arg_] | [string match "false" $arg_] | [string match {[01]} $arg_]} break
		incr i
	}
	set j $i; incr j -1;
	set flagNames_ [lrange $args_ 0 $j]

	set cmd_ {set flagNames_ [java::new String\[\] $i }
	append cmd_ \{
	foreach flagName_ $flagNames_ { append cmd_ " $flagName_" }
	append cmd_ \}\]
	#puts "i=$i, j=$j"
	#puts "flag names = $flagNames_"
	#puts "cmd = '$cmd_'"
	# the following does not work??
	#eval [subst -nocommands {set flagNames_ [java::new String\[\] $i {$flagNames_}]}]
	eval $cmd_
	#puts here

	set enabled_ [lindex $args_ $i]; incr i
	set tmp_ [lindex $args_ $i]
	set recursive_ ""
	set debug_flags [java::null]
	if [string match "recursively" $tmp_] {
		set recursive_ true
		incr i
		set tmp_ [lindex $args_ $i]
	}

	set overwrite_ false
	if {[string match "-at" $tmp_] || [string match "-onlyat" $tmp_]} {
		set overwrite_ [string match "-onlyat" $tmp_]
		# check debug flags
		incr i
		set debug_flags [lindex $args_ $i]; # should be a list of integers/strings
		#puts "debug flags: $debug_flags"
		incr i
		if [string match {[0-9]} [lindex $debug_flags 0]] {
			set debug_flags [java::new {int[]} [llength $debug_flags] $debug_flags]
		} else {
			set debug_flags [_to_string_array $debug_flags]
		}
	} elseif [string match "-only" $tmp_] {
		set overwrite_ 1
	}

	if {$i == [llength $args_]-1} {
		set args_ [list [lindex $args_ end]]
	} else {
		set args_ [lrange $args_ $i end]
	}
	#puts "targets = $args_"

	global _pwd_ __shell
	set jpaths_ [_to_Paths $_pwd_ $args_]
	if {$recursive_ == ""} {
		java::call drcl.ruv.Commands "setflag" $flags_ $flagNames_ $enabled_ $debug_flags $overwrite_ $jpaths_ $__shell
	} else {
		java::call drcl.ruv.Commands "setflag" $flags_ $flagNames_ $enabled_ $recursive_ $debug_flags $overwrite_ $jpaths_ $__shell
	}
}

# display component flags
proc getflag args { __common__ "getflag" $args }

# -label <label>
# -add/-remove
# syntax: watch ?-c? ?-label <label>? -add|-remove ... 
# <label> can be "none" for default watcher label
# e.g. watch -c -add x/1@ -remove y/2@ -label case2 -add z/3@ -label none -remove q/3@
# it will attach a watcher to x/1@ with the default watcher label and remove watchers
# (with the default watcher label) # of y/2@ and q/3@, and attach watcher to z/3@ with
# label "case2"
proc watch args {
	array set argArray_ [_parse2 $args "-add -remove -label"]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)

	set op_ ""
	set label_ "none"
	set prep_ ""
	set startIndex_ ""
	set endIndex_ ""
	set var_ ""
	set complete_ 0
	set i -1
	global _system_monitor_ _system_monitor_watch_port_
	foreach arg_ $args_ {
		incr i
		#puts "arg $i: '$arg_'"
		if {$var_ == ""} {
			# check if a subcommand is complete
			if [string match "-label" $arg_] {
				set var_ "label_"
				set complete_ 1
			} elseif [string match "-add" $arg_] {
				set prep_ "-to"
				set complete_ 1
			} elseif [string match "-remove" $arg_] {
				set prep_ "-to"
				set complete_ 1
			} elseif {$i == [llength $args_]-1} {
				incr i; # endIndex_ will be set to (i-1)
				set complete_ 1
			}

			if {$complete_} {
				#puts "complete at index $i, startIndex = '$startIndex_'"
				set complete_ 0
				if {$startIndex_ != ""} {
					set endIndex_ [expr $i-1]
					if [string match "none" $label_] {
						set watcher_ $_system_monitor_/$_system_monitor_watch_port_\@
					} else {
						set watcher_ $_system_monitor_/$label_\@
						[mkdir -q $watcher_] setType [java::field drcl.comp.Port PortType_IN]
					}
					set cmd_ [subst {$op_ -$flags_ $watcher_ $prep_ [lrange $args_ $startIndex_ $endIndex_]}]
					#puts $cmd_
					eval $cmd_
					set startIndex_ ""
				}
			}

			if [string match "-add" $arg_] {
				set startIndex_ [expr $i+1]
				set op_ "attach"
			} elseif [string match "-remove" $arg_] {
				set startIndex_ [expr $i+1]
				set op_ "detach"
			}
		} else {
			set $var_ $arg_
			set var_ ""
		}
	}
}

proc autocomplete args { return [__common__ "autocomplete" $args] }
proc explore args { __common__ "explore" $args }
proc verify args { __common__ "verify" $args }

# script <script> ?args...?
# Args:
# -at <time> ?later?: default is 0.0.
# -period <period>: default is -1.0.
# -on <runtime_instance>: default is the default runtime.
# -shell <shell_instance>: default is the current shell.
proc script args {
	set sim_ [java::field drcl.comp.ACARuntime DEFAULT_RUNTIME]
	set time_ 0.0
	set period_ -1.0
	global __shell
	set shell_ $__shell
	set script_ {puts "No script is specified."}
	set method_ "add"

	for {set i 0} {$i < [llength $args]} {incr i} {
		set arg_ [lindex $args $i]
		if [string match "-on" $arg_] {
			incr i; set arg_ [lindex $args $i]
			set sim_ [! $arg_]
		} elseif [string match "-at" $arg_] {
			incr i; set arg_ [lindex $args $i]
			set method_ "addAt"
			set time_ $arg_
		} elseif [string match "later" $arg_] {
			set method_ "add"
		} elseif [string match "-period" $arg_] {
			incr i; set arg_ [lindex $args $i]
			set period_ $arg_
		} elseif [string match "-shell" $arg_] {
			incr i; set arg_ [lindex $args $i]
			set shell_ [! $arg_]
		} else {
			set script_ $arg_
		}
	}
	set task_ [java::call drcl.ruv.CommandTask $method_ $script_ $time_ $period_ $sim_ $shell_]
	return $task_
}

# The function will busy-wait until the condition obtained by executing the command becomes true.
# This function only works as a single statement as the execution control is at the granuality of
# statement.
#
# cmd_: command which returns the condition whether to escape from the waiting loop or not.
proc wait_until cmd_ {
	global __shell
	java::new drcl.ruv.WaitUntil $__shell $cmd_
}

# open a terminal
# term title [-t term_class] [-s shell_class/shell_object] [init script]
proc term {title_ args} {
	global _gsys_

	set termClass_ ""
	set shell_ ""
	set shellClass_ ""
	set flag_ ""
	set initScript_ ""
	foreach arg_ $args {
		if {[string first "-" $arg_] == 0} {
			set flag_ $arg_
			continue
		}
		if [string match "-t" $flag_] {
			set termClass_ $arg_
		} elseif [string match "-s" $flag_] {
			set shell_ $arg_
			if [catch {java::instanceof $shell_ Object}] {
				# not a Java object
				set shellClass_ $arg_
				set shell_ ""
			} elseif {![java::instanceof $shell_ drcl.ruv.Shell]} {
				# not a Shell
				puts "'[$shell_ toString]' is not a Shell object!"
				return
			}
		} else {
			# init script
			set initScript_ $arg_
		}
	}

	if {![java::isnull [[! /.term] getComponent $title_]]} {
		puts "The terminal titled '$title_' already exists."
		! /.term/$title_ show
		return
	}

	# create and init the shell
	set shellid_ "$title_\_shell"
	if {$shell_ == ""} {
		if [java::isnull [! -q /.term/$shellid_]] {
			if {$shellClass_ == ""} {
				set shell_ [java::new drcl.ruv.ShellTcl $shellid_]
			} elseif [catch {set shell_ [java::new $shellClass_]}] {
				puts "$shellClass_: shell class does not exist."
				return
			}
			mkdir $shell_ /.term/$shellid_
		} else {
			set shell_ [! /.term/$shellid_]
		}
	}

	# create and init the term
	if {$termClass_ == ""} {
		set t_ [java::new drcl.ruv.Dterm $title_]
	} elseif [catch {set t_ [java::new $termClass_]}] {
		puts "$termClass_: terminal class does not exist."
		return
	}
	if [java::isnull [$t_ getParent]] {
		mkdir $t_ /.term/$title_
	}

	! /.system addTerminal $t_ $shell_ $initScript_ [java::null]
	#XXX: should copy the variables from this shell to the new shell
}

# Returns the active terminal
proc _getTerminal {} {
	global __shell
	return [java::call drcl.ruv.System getTerminal $__shell]
}

proc exit {} {
	set this_ [_getTerminal]
	if {![java::isnull $this_]} { $this_ exit }
}

proc quit {} {
	set this_ [_getTerminal]
	if {![java::isnull $this_]} {
		$this_ quit
	}
}


proc _getRuntime comp_ {
	set comp_ [! $comp_]
	return [java::call drcl.comp.Util getRuntime $comp_]
}

proc _getForkManager comp_ {
	set comp_ [! $comp_]
	return [java::call drcl.comp.Util getForkManager $comp_]
}

# ---------------------------------------
# Utility for setting up runtime
# ---------------------------------------

# Creates a (default) runtime instance and attaches it to the components.
# @param args	?workforce, default 1?, list of component
# @return	the runtime
proc attach_runtime args {
	set workforce_ [lindex $args 0]
	if [catch {set workforce_ [expr $workforce_ + 0]}] {
		set start_ 0
		set workforce_ 1
	} else {
		set start_ 1
	}

	if [catch {java::call Class forName [lindex $args $start_]}] {
		set runtime_ [java::new drcl.comp.ARuntime]
	} else {
		set runtime_ [java::new [lindex $args $start_]]
		incr start_
	}

	catch {$runtime_ setMaxWorkforce $workforce_}

	for {set i $start_} {$i < [llength $args]} {incr i} {
		$runtime_ takeover [! [lindex $args $i]]
	}

	return $runtime_
}

# Creates a default simulator instance and attaches it to the components.
# @param args	?workforce, default 1?, list of component
# @return	the simulator
proc attach_simulator args {
	set workforce_ [lindex $args 0]
	if [string match $workforce_ event] {
		set start_ 1
		set sim_ [java::new drcl.sim.event.SESimulator]
	} elseif [string match $workforce_ event-old] {
		set start_ 1
		set sim_ [java::new drcl.sim.event.SESimulatorOld]
	} elseif [catch {set workforce_ [expr $workforce_ + 0]}] {
		set start_ 0
		set sim_ [!!! [java::call drcl.sim.SimulatorAssistant defaultInstance]]
		$sim_ setMaxWorkforce 1
	} else {
		set start_ 1
		set sim_ [!!! [java::call drcl.sim.SimulatorAssistant defaultInstance]]
		$sim_ setMaxWorkforce $workforce_
	}

	for {set i $start_} {$i < [llength $args]} {incr i} {
		$sim_ takeover [! [lindex $args $i]]
	}

	return $sim_
}

# ---------------------------------------
# Utility for processing a chunk of text
# ---------------------------------------

# Searches a pattern in a big chunk of text and returns the lines that
# contains the pattern.
#
# pattern_: the pattern.
# content_: the string.
# return: the new string consisting of the lines that contain the pattern.
proc grep {pattern_ content_} {
	set lines_ [split $content_ \n]
	set result_ ""
	foreach ll $lines_ {
		if [string match "*$pattern_\*" $ll] { append result_ "$ll\n" }
	}
	return $result_
}

# Extracts lines of text from the given text.
#
# content_: the original string.
# start_: the start line index, inclusive.
# end_: the end line index, inclusive.
# return: the new text.
proc subtext {content_ start_ end_} {
	set lines_ [split $content_ \n]
	return [join [lrange $lines_ $start_ $end_] \n]
}

# Returns the number of lines in the given text
#
# content_: the string.
# return: the number of lines.
proc nlines {content_} {
	return [llength [split $content_ \n]]
}

#if 1 {
#$__shell evalResource string.tcl
#$__shell evalResource sim.tcl
#}

proc _rt_common_ {cmd_ args_} {
	array set argArray_ [_parse $args_]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)
	if {[llength $args_] == 0} return

	set path_ [lindex $args_ 0]
	set args_ [lrange $args_ 1 end]

	set target_ [! -q$flags_ $path_]
	#puts [$target_ toString]
	if [java::isnull $target_] {
		#puts "Invalid path: $path_"
		return
	}
	if [java::instanceof $target_ {Object[]}] {
		#puts "object array"
		set len_ [$target_ length]
		if {$len_ == 1} {
			set target_ [$target_ get 0]
		} else {
			set result_ ""
			for {set i 0} {$i < $len_} {incr i} {
				#puts $i
				set comp_ [$target_ get $i]
				if [string match "rt" $cmd_] {
					set rt_ [java::call drcl.comp.Util getRuntime $comp_]
				} else {
					set rt_ [java::call drcl.comp.Util getForkManager $comp_]
				}
				!!! rt_
				if {[llength $args_] == 0} {
					if [string match "rt" $cmd_] {
						append result_ [$rt_ info]
					} else {
						append result_ [$rt_ info false]
					}
				} else {
					append result_ [eval [subst -nocommands "$rt_ $args_"]]
				}
			}
			return $result_
		}
	}
	if [string match "rt" $cmd_] {
		set rt_ [java::call drcl.comp.Util getRuntime $target_]
	} else {
		set rt_ [java::call drcl.comp.Util getForkManager $target_]
	}
	!!! rt_
	if {[llength $args_] == 0} {
		if {[string first q $flags_] < 0} {
			if [string match "rt" $cmd_] {
				puts [$rt_ info]
			} else {
				puts [$rt_ info false]
			}
		}
		return $rt_
	} else {
		eval [subst -nocommands "$rt_ $args_"]
	}
}

proc rt args { _rt_common_ "rt" $args }
proc fm args { _rt_common_ "fm" $args }

proc reset args {
	array set argArray_ [_parse $args]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)

	if {[llength $args_] == 0} {
		java::call drcl.ruv.System resetSystem
	} else {
		foreach arg_ $args_ {
			! -$flags_ $arg_ reset
		}
	}
}

proc reboot args {
	array set argArray_ [_parse $args]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)

	foreach arg_ $args_ {
		! -$flags_ $arg_ reboot
	}
}

proc _operate {cmd_ args} {
	array set argArray_ [_parse $args]
	set flags_ $argArray_(flags)
	set args_ [join $argArray_(args) " "]

	set objs_ [eval [subst -nocommands {!!s -$flags_ $args_}]]
	java::call drcl.comp.Util operate $objs_ $cmd_
}

proc run args { _operate "start" $args }
proc stop args { _operate "stop" $args }
proc resume args { _operate "resume" $args }

proc inject args {
	array set argArray_ [_parse $args]
	set flags_ $argArray_(flags)
	set args_ $argArray_(args)
	if {[llength $args_] == 0} return

	set data_ [lindex $args_ 0]
	set args_ [lrange $args_ 1 end]
	foreach arg_ $args_ {
		set arg_ [!! -q$flags_ $arg_]
		java::call drcl.comp.Util inject $data_ $arg_
		#set arg_ [! -q$flags_ $arg_]
		#if [java::instanceof $arg_ {Object[]}] {
		#	set len_ [$arg_ length]
		#	for {set i 0} {$i < $len_} {incr i} {
		#		inject -$flags_ $data_ [$arg_ get $i]
		#	}
		#} elseif [java::instanceof $arg_ drcl.comp.Port] {
		#	$arg_ doReceiving $data_
		#}
	}
}

proc jsim {} { return "J-Sim v1.3" }


